from datetime import datetime, timedelta, timezone
from typing import Optional

import jwt
from fastapi import Depends, FastAPI, HTTPException, WebSocket, WebSocketDisconnect
from fastapi.middleware.cors import CORSMiddleware
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from passlib.context import CryptContext
from pydantic import BaseModel
from sqlalchemy import Boolean, DateTime, ForeignKey, Integer, String, Text, create_engine, select
from sqlalchemy.orm import DeclarativeBase, Mapped, Session, mapped_column

SECRET = "dev-secret"
ALGO = "HS256"
TOKEN_MIN = 60 * 24

app = FastAPI(title="DiceRpg API", version="0.1.0")
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

engine = create_engine("sqlite:///./dicerpg.db", connect_args={"check_same_thread": False})
pwd = CryptContext(schemes=["bcrypt"], deprecated="auto")
security = HTTPBearer()


class Base(DeclarativeBase):
    pass


class User(Base):
    __tablename__ = "users"
    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    email: Mapped[str] = mapped_column(String(120), unique=True)
    username: Mapped[str] = mapped_column(String(60), unique=True)
    password_hash: Mapped[str] = mapped_column(String(200))


class Campaign(Base):
    __tablename__ = "campaigns"
    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    name: Mapped[str] = mapped_column(String(120))
    description: Mapped[str] = mapped_column(Text, default="")
    owner_id: Mapped[int] = mapped_column(ForeignKey("users.id"))
    is_public: Mapped[bool] = mapped_column(Boolean, default=False)


class Membership(Base):
    __tablename__ = "memberships"
    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    campaign_id: Mapped[int] = mapped_column(ForeignKey("campaigns.id"))
    user_id: Mapped[int] = mapped_column(ForeignKey("users.id"))
    role: Mapped[str] = mapped_column(String(20), default="player")


class Character(Base):
    __tablename__ = "characters"
    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    campaign_id: Mapped[int] = mapped_column(ForeignKey("campaigns.id"))
    user_id: Mapped[int] = mapped_column(ForeignKey("users.id"))
    name: Mapped[str] = mapped_column(String(120))
    sheet_json: Mapped[str] = mapped_column(Text, default='{"sheet":{},"inventory":[],"log":[]}')


class Message(Base):
    __tablename__ = "messages"
    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    campaign_id: Mapped[int] = mapped_column(ForeignKey("campaigns.id"))
    sender_id: Mapped[int] = mapped_column(ForeignKey("users.id"))
    persona_type: Mapped[str] = mapped_column(String(20), default="user")
    persona_name: Mapped[str] = mapped_column(String(120), default="")
    text: Mapped[str] = mapped_column(Text)
    created_at: Mapped[datetime] = mapped_column(DateTime, default=lambda: datetime.now(timezone.utc))


Base.metadata.create_all(engine)


class RegisterIn(BaseModel):
    email: str
    username: str
    password: str


class LoginIn(BaseModel):
    email: str
    password: str


class CampaignIn(BaseModel):
    name: str
    description: str = ""
    is_public: bool = False


class CharacterIn(BaseModel):
    campaign_id: int
    name: str
    sheet_json: str = '{"sheet":{},"inventory":[],"log":[]}'


class MessageIn(BaseModel):
    text: str
    persona_type: str = "user"
    persona_name: Optional[str] = ""


def token_for(uid: int) -> str:
    payload = {"sub": uid, "exp": datetime.now(timezone.utc) + timedelta(minutes=TOKEN_MIN)}
    return jwt.encode(payload, SECRET, algorithm=ALGO)


def current_user(creds: HTTPAuthorizationCredentials = Depends(security)) -> User:
    try:
        payload = jwt.decode(creds.credentials, SECRET, algorithms=[ALGO])
    except jwt.PyJWTError:
        raise HTTPException(status_code=401, detail="invalid token")
    with Session(engine) as s:
        user = s.get(User, payload.get("sub"))
        if not user:
            raise HTTPException(status_code=401, detail="user not found")
        return user


@app.get("/health")
def health():
    return {"status": "ok"}


@app.post("/auth/register")
def register(data: RegisterIn):
    with Session(engine) as s:
        if s.scalar(select(User).where(User.email == data.email)):
            raise HTTPException(status_code=400, detail="email exists")
        user = User(email=data.email, username=data.username, password_hash=pwd.hash(data.password))
        s.add(user)
        s.commit()
        s.refresh(user)
        return {"token": token_for(user.id)}


@app.post("/auth/login")
def login(data: LoginIn):
    with Session(engine) as s:
        user = s.scalar(select(User).where(User.email == data.email))
        if not user or not pwd.verify(data.password, user.password_hash):
            raise HTTPException(status_code=401, detail="invalid credentials")
        return {"token": token_for(user.id)}


@app.post("/campaigns")
def create_campaign(data: CampaignIn, user: User = Depends(current_user)):
    with Session(engine) as s:
        c = Campaign(name=data.name, description=data.description, owner_id=user.id, is_public=data.is_public)
        s.add(c)
        s.flush()
        s.add(Membership(campaign_id=c.id, user_id=user.id, role="gm"))
        s.commit()
        s.refresh(c)
        return {"id": c.id, "name": c.name, "description": c.description, "is_public": c.is_public}


@app.get("/campaigns")
def list_campaigns(user: User = Depends(current_user)):
    with Session(engine) as s:
        rows = s.execute(select(Campaign).join(Membership, Membership.campaign_id == Campaign.id).where(Membership.user_id == user.id)).scalars().all()
        return [{"id": c.id, "name": c.name, "description": c.description, "is_public": c.is_public} for c in rows]


@app.post("/characters")
def create_character(data: CharacterIn, user: User = Depends(current_user)):
    with Session(engine) as s:
        m = s.scalar(select(Membership).where(Membership.campaign_id == data.campaign_id, Membership.user_id == user.id))
        if not m:
            raise HTTPException(status_code=403, detail="not member")
        ch = Character(campaign_id=data.campaign_id, user_id=user.id, name=data.name, sheet_json=data.sheet_json)
        s.add(ch)
        s.commit()
        s.refresh(ch)
        return {"id": ch.id, "name": ch.name, "sheet_json": ch.sheet_json}


@app.get("/campaigns/{campaign_id}/characters")
def list_characters(campaign_id: int, user: User = Depends(current_user)):
    with Session(engine) as s:
        m = s.scalar(select(Membership).where(Membership.campaign_id == campaign_id, Membership.user_id == user.id))
        if not m:
            raise HTTPException(status_code=403, detail="not member")
        rows = s.scalars(select(Character).where(Character.campaign_id == campaign_id)).all()
        return [{"id": c.id, "name": c.name, "user_id": c.user_id, "sheet_json": c.sheet_json} for c in rows]


@app.post("/campaigns/{campaign_id}/messages")
def send_message(campaign_id: int, data: MessageIn, user: User = Depends(current_user)):
    with Session(engine) as s:
        m = s.scalar(select(Membership).where(Membership.campaign_id == campaign_id, Membership.user_id == user.id))
        if not m:
            raise HTTPException(status_code=403, detail="not member")
        msg = Message(campaign_id=campaign_id, sender_id=user.id, text=data.text, persona_type=data.persona_type, persona_name=data.persona_name or user.username)
        s.add(msg)
        s.commit()
        s.refresh(msg)
        return {"id": msg.id, "text": msg.text, "persona_type": msg.persona_type, "persona_name": msg.persona_name}


@app.get("/campaigns/{campaign_id}/messages")
def list_messages(campaign_id: int, user: User = Depends(current_user)):
    with Session(engine) as s:
        m = s.scalar(select(Membership).where(Membership.campaign_id == campaign_id, Membership.user_id == user.id))
        if not m:
            raise HTTPException(status_code=403, detail="not member")
        msgs = s.scalars(select(Message).where(Message.campaign_id == campaign_id).order_by(Message.id.desc()).limit(50)).all()
        return [{"id": x.id, "text": x.text, "persona_type": x.persona_type, "persona_name": x.persona_name, "created_at": x.created_at.isoformat()} for x in reversed(msgs)]


class RoomHub:
    def __init__(self):
        self.clients: dict[int, list[WebSocket]] = {}

    async def connect(self, campaign_id: int, ws: WebSocket):
        await ws.accept()
        self.clients.setdefault(campaign_id, []).append(ws)

    def disconnect(self, campaign_id: int, ws: WebSocket):
        if campaign_id in self.clients and ws in self.clients[campaign_id]:
            self.clients[campaign_id].remove(ws)

    async def broadcast(self, campaign_id: int, payload: dict):
        for ws in list(self.clients.get(campaign_id, [])):
            await ws.send_json(payload)


hub = RoomHub()


@app.websocket("/ws/campaigns/{campaign_id}")
async def ws_campaign(campaign_id: int, ws: WebSocket):
    await hub.connect(campaign_id, ws)
    try:
        while True:
            data = await ws.receive_json()
            await hub.broadcast(campaign_id, data)
    except WebSocketDisconnect:
        hub.disconnect(campaign_id, ws)
