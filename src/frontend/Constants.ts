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

export type PlayerId = string
export type PlayerIds = PlayerId[]
export type ActualBids = {[playerId: PlayerId]: number}