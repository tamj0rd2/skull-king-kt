export type Card =
  | Card.NumberedCard
  | Card.SpecialCard

export namespace Card {
  export enum Type {
    NumberedCard = "com.tamj0rd2.domain.Card.NumberedCard",
    SpecialCard = "com.tamj0rd2.domain.Card.SpecialCard",
  }
  
  export interface NumberedCard {
    type: Card.Type.NumberedCard
    name: string
    suit: Suit
    number: number
  }
  
  export interface SpecialCard {
    type: Card.Type.SpecialCard
    name: string
    suit: SpecialSuit
  }
}

export enum Suit {
  Red = "Red",
  Yellow = "Yellow",
  Blue = "Blue",
  Black = "Black",
}

export enum SpecialSuit {
  Escape = "Escape",
  Pirate = "Pirate",
  Mermaid = "Mermaid",
  SkullKing = "SkullKing",
}

export interface CardWithPlayability {
  card: Card
  isPlayable: boolean
}

export type PlayerCommand =
  | PlayerCommand.JoinGame
  | PlayerCommand.PlaceBid
  | PlayerCommand.PlayCard

export namespace PlayerCommand {
  export enum Type {
    JoinGame = "com.tamj0rd2.domain.PlayerCommand.JoinGame",
    PlaceBid = "com.tamj0rd2.domain.PlayerCommand.PlaceBid",
    PlayCard = "com.tamj0rd2.domain.PlayerCommand.PlayCard",
  }
  
  export interface JoinGame {
    type: PlayerCommand.Type.JoinGame
    actor: string
  }
  
  export interface PlaceBid {
    type: PlayerCommand.Type.PlaceBid
    actor: string
    bid: number
  }
  
  export interface PlayCard {
    type: PlayerCommand.Type.PlayCard
    actor: string
    cardName: string
  }
}

export enum GameState {
  WaitingForMorePlayers = "WaitingForMorePlayers",
  WaitingToStart = "WaitingToStart",
  InProgress = "InProgress",
  Complete = "Complete",
}

export enum RoundPhase {
  Bidding = "Bidding",
  BiddingCompleted = "BiddingCompleted",
  TrickTaking = "TrickTaking",
  TrickCompleted = "TrickCompleted",
}

export type Notification =
  | Notification.BidPlaced
  | Notification.BiddingCompleted
  | Notification.CardPlayed
  | Notification.GameCompleted
  | Notification.GameStarted
  | Notification.PlayerJoined
  | Notification.RoundCompleted
  | Notification.RoundStarted
  | Notification.TrickCompleted
  | Notification.TrickStarted
  | Notification.YouJoined
  | Notification.YourTurn

export namespace Notification {
  export enum Type {
    BidPlaced = "com.tamj0rd2.webapp.Notification.BidPlaced",
    BiddingCompleted = "com.tamj0rd2.webapp.Notification.BiddingCompleted",
    CardPlayed = "com.tamj0rd2.webapp.Notification.CardPlayed",
    GameCompleted = "com.tamj0rd2.webapp.Notification.GameCompleted",
    GameStarted = "com.tamj0rd2.webapp.Notification.GameStarted",
    PlayerJoined = "com.tamj0rd2.webapp.Notification.PlayerJoined",
    RoundCompleted = "com.tamj0rd2.webapp.Notification.RoundCompleted",
    RoundStarted = "com.tamj0rd2.webapp.Notification.RoundStarted",
    TrickCompleted = "com.tamj0rd2.webapp.Notification.TrickCompleted",
    TrickStarted = "com.tamj0rd2.webapp.Notification.TrickStarted",
    YouJoined = "com.tamj0rd2.webapp.Notification.YouJoined",
    YourTurn = "com.tamj0rd2.webapp.Notification.YourTurn",
  }
  
  export interface BidPlaced {
    type: Notification.Type.BidPlaced
    playerId: string
  }
  
  export interface BiddingCompleted {
    type: Notification.Type.BiddingCompleted
    bids: { [key: string]: number }
  }
  
  export interface CardPlayed {
    type: Notification.Type.CardPlayed
    playerId: string
    card: Card
    nextPlayer: string | null
  }
  
  export interface GameCompleted {
    type: Notification.Type.GameCompleted
  }
  
  export interface GameStarted {
    type: Notification.Type.GameStarted
    players: string[]
  }
  
  export interface PlayerJoined {
    type: Notification.Type.PlayerJoined
    playerId: string
    waitingForMorePlayers: boolean
  }
  
  export interface RoundCompleted {
    type: Notification.Type.RoundCompleted
    wins: { [key: string]: number }
  }
  
  export interface RoundStarted {
    type: Notification.Type.RoundStarted
    cardsDealt: CardWithPlayability[]
    roundNumber: number
  }
  
  export interface TrickCompleted {
    type: Notification.Type.TrickCompleted
    winner: string
  }
  
  export interface TrickStarted {
    type: Notification.Type.TrickStarted
    trickNumber: number
    firstPlayer: string
  }
  
  export interface YouJoined {
    type: Notification.Type.YouJoined
    playerId: string
    players: string[]
    waitingForMorePlayers: boolean
  }
  
  export interface YourTurn {
    type: Notification.Type.YourTurn
    cards: CardWithPlayability[]
  }
}

export type Message =
  | Message.AckFromClient
  | Message.AckFromServer
  | Message.KeepAlive
  | Message.Nack
  | Message.ToClient
  | Message.ToServer

export namespace Message {
  export enum Type {
    AckFromClient = "com.tamj0rd2.webapp.Message.AckFromClient",
    AckFromServer = "com.tamj0rd2.webapp.Message.AckFromServer",
    KeepAlive = "com.tamj0rd2.webapp.Message.KeepAlive",
    Nack = "com.tamj0rd2.webapp.Message.Nack",
    ToClient = "com.tamj0rd2.webapp.Message.ToClient",
    ToServer = "com.tamj0rd2.webapp.Message.ToServer",
  }
  
  export interface AckFromClient {
    type: Message.Type.AckFromClient
    id: string
  }
  
  export interface AckFromServer {
    type: Message.Type.AckFromServer
    id: string
    notifications: Notification[]
  }
  
  export interface KeepAlive {
    type: Message.Type.KeepAlive
    id: string
  }
  
  export interface Nack {
    type: Message.Type.Nack
    id: string
    reason: string
  }
  
  export interface ToClient {
    type: Message.Type.ToClient
    notifications: Notification[]
    id: string
  }
  
  export interface ToServer {
    type: Message.Type.ToServer
    command: PlayerCommand
    id: string
  }
}