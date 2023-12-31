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

export enum GamePhase {
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
    state: PlayerState
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
    state: PlayerState
    id: TamId
  }
  
  export interface ToServer {
    type: Message.Type.ToServer
    command: PlayerCommand
    id: TamId
  }
}

export type TamId = string

export interface PlayerState {
  playerId: PlayerId
  winsOfTheRound?: { [key: PlayerId]: number }
  trickWinner?: PlayerId | null
  currentPlayer?: PlayerId | null
  trickNumber?: TrickNumber
  roundNumber?: RoundNumber
  trick?: Trick | null
  roundPhase?: RoundPhase | null
  gamePhase?: GamePhase | null
  playersInRoom?: PlayerId[]
  hand?: CardWithPlayability[]
  bids?: { [key: PlayerId]: DisplayBid }
  turnOrder?: PlayerId[]
  currentSuit?: Suit | null
}

export type TrickNumber = number

export type RoundNumber = number

export interface Trick {
  size: number
  playedCards: PlayedCard[]
  suit?: Suit | null
  hasSkullKing?: boolean
  hasMermaid?: boolean
  hasPirate?: boolean
}

export type DisplayBid =
  | DisplayBid.Hidden
  | DisplayBid.None
  | DisplayBid.Placed

export namespace DisplayBid {
  export enum Type {
    Hidden = "com.tamj0rd2.domain.DisplayBid.Hidden",
    None = "com.tamj0rd2.domain.DisplayBid.None",
    Placed = "com.tamj0rd2.domain.DisplayBid.Placed",
  }
  
  export interface Hidden {
    type: DisplayBid.Type.Hidden
  }
  
  export interface None {
    type: DisplayBid.Type.None
  }
  
  export interface Placed {
    type: DisplayBid.Type.Placed
    bid: Bid
  }
}

export interface PlayedCard {
  playerId: PlayerId
  card: Card
}
