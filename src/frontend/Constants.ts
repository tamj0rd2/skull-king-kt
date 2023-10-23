declare global {
    const INITIAL_STATE: {
        endpoint: string
        ackTimeoutMs: number
    }
}

export const enum GameState {
    WaitingForMorePlayers = "WaitingForMorePlayers",
    WaitingToStart = "WaitingToStart",
    InProgress = "InProgress",
    Complete = "Complete",
}

export const enum GamePhase {
    Bidding = "Bidding",
    BiddingCompleted = "BiddingCompleted",
    TrickTaking = "TrickTaking",
    TrickCompleted = "TrickCompleted",
}

export const enum CardType {
    NumberedCard = "Card$NumberedCard",
    SpecialCard = "Card$SpecialCard",
}

export interface Card {
    suit: string
    number?: number
    name: string
    type: CardType
}

export type PlayerId = string
export type PlayerIds = PlayerId[]
export type ActualBids = {[playerId: PlayerId]: number}

export enum NotificationType {
    PlayerJoined = "Notification$PlayerJoined",
    GameStarted = "Notification$GameStarted",
    RoundStarted = "Notification$RoundStarted",
    BidPlaced = "Notification$BidPlaced",
    BiddingCompleted = "Notification$BiddingCompleted",
    CardPlayed = "Notification$CardPlayed",
    TrickCompleted = "Notification$TrickCompleted",
    TrickStarted = "Notification$TrickStarted",
    GameCompleted = "Notification$GameCompleted",
    YourTurn = "Notification$YourTurn",
    RoundCompleted = "Notification$RoundCompleted",
    YouJoined = "Notification$YouJoined",
}

export const knownNotificationTypes = Object.values(NotificationType)

export enum CommandType {
    PlaceBid = "Command$PlayerCommand$PlaceBid",
    PlayCard = "Command$PlayerCommand$PlayCard",
    // UnhandledServerMessage = "MessageFromClient$UnhandledServerMessage",
    // Error = "MessageFromClient$Error",
    JoinGame = "Command$PlayerCommand$JoinGame"
}

export interface Command {
    type: CommandType
    actor: PlayerId
    [key: string]: any
}
