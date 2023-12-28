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
    name: CardName
    suit: Suit
    number: number
  }
  
  export interface SpecialCard {
    type: Card.Type.SpecialCard
    name: CardName
    suit: SpecialSuit
  }
}

export type CardName = string

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
    actor: PlayerId
  }
  
  export interface PlaceBid {
    type: PlayerCommand.Type.PlaceBid
    actor: PlayerId
    bid: Bid
  }
  
  export interface PlayCard {
    type: PlayerCommand.Type.PlayCard
    actor: PlayerId
    cardName: CardName
  }
}

export type PlayerId = string

export type Bid = number

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
    BidPlaced = "com.tamj0rd2.messaging.Notification.BidPlaced",
    BiddingCompleted = "com.tamj0rd2.messaging.Notification.BiddingCompleted",
    CardPlayed = "com.tamj0rd2.messaging.Notification.CardPlayed",
    GameCompleted = "com.tamj0rd2.messaging.Notification.GameCompleted",
    GameStarted = "com.tamj0rd2.messaging.Notification.GameStarted",
    PlayerJoined = "com.tamj0rd2.messaging.Notification.PlayerJoined",
    RoundCompleted = "com.tamj0rd2.messaging.Notification.RoundCompleted",
    RoundStarted = "com.tamj0rd2.messaging.Notification.RoundStarted",
    TrickCompleted = "com.tamj0rd2.messaging.Notification.TrickCompleted",
    TrickStarted = "com.tamj0rd2.messaging.Notification.TrickStarted",
    YouJoined = "com.tamj0rd2.messaging.Notification.YouJoined",
    YourTurn = "com.tamj0rd2.messaging.Notification.YourTurn",
  }
  
  export interface BidPlaced {
    type: Notification.Type.BidPlaced
    playerId: PlayerId
  }
  
  export interface BiddingCompleted {
    type: Notification.Type.BiddingCompleted
    bids: { [key: PlayerId]: Bid }
  }
  
  export interface CardPlayed {
    type: Notification.Type.CardPlayed
    playerId: PlayerId
    card: Card
    nextPlayer: PlayerId | null
  }
  
  export interface GameCompleted {
    type: Notification.Type.GameCompleted
  }
  
  export interface GameStarted {
    type: Notification.Type.GameStarted
    players: PlayerId[]
  }
  
  export interface PlayerJoined {
    type: Notification.Type.PlayerJoined
    playerId: PlayerId
    waitingForMorePlayers: boolean
  }
  
  export interface RoundCompleted {
    type: Notification.Type.RoundCompleted
    wins: { [key: PlayerId]: number }
  }
  
  export interface RoundStarted {
    type: Notification.Type.RoundStarted
    cardsDealt: CardWithPlayability[]
    roundNumber: RoundNumber
  }
  
  export interface TrickCompleted {
    type: Notification.Type.TrickCompleted
    winner: PlayerId
  }
  
  export interface TrickStarted {
    type: Notification.Type.TrickStarted
    trickNumber: TrickNumber
    firstPlayer: PlayerId
  }
  
  export interface YouJoined {
    type: Notification.Type.YouJoined
    playerId: PlayerId
    players: PlayerId[]
    waitingForMorePlayers: boolean
  }
  
  export interface YourTurn {
    type: Notification.Type.YourTurn
    cards: CardWithPlayability[]
  }
}

export type RoundNumber = number

export type TrickNumber = number

export type Message =
  | Message.AcceptanceFromClient
  | Message.AcceptanceFromServer
  | Message.KeepAlive
  | Message.Rejection
  | Message.ToClient
  | Message.ToServer

export namespace Message {
  export enum Type {
    AcceptanceFromClient = "com.tamj0rd2.messaging.Message.AcceptanceFromClient",
    AcceptanceFromServer = "com.tamj0rd2.messaging.Message.AcceptanceFromServer",
    KeepAlive = "com.tamj0rd2.messaging.Message.KeepAlive",
    Rejection = "com.tamj0rd2.messaging.Message.Rejection",
    ToClient = "com.tamj0rd2.messaging.Message.ToClient",
    ToServer = "com.tamj0rd2.messaging.Message.ToServer",
  }
  
  export interface AcceptanceFromClient {
    type: Message.Type.AcceptanceFromClient
    id: TamId
  }
  
  export interface AcceptanceFromServer {
    type: Message.Type.AcceptanceFromServer
    id: TamId
    notifications: Notification[]
  }
  
  export interface KeepAlive {
    type: Message.Type.KeepAlive
    id: TamId
  }
  
  export interface Rejection {
    type: Message.Type.Rejection
    id: TamId
    reason: string
  }
  
  export interface ToClient {
    type: Message.Type.ToClient
    notifications: Notification[]
    id: TamId
  }
  
  export interface ToServer {
    type: Message.Type.ToServer
    command: PlayerCommand
    id: TamId
  }
}

export type TamId = string
