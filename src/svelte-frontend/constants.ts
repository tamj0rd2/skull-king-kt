export const enum GameState {
    WaitingForMorePlayers = "WaitingForMorePlayers",
    WaitingToStart = "WaitingToStart",
    InProgress = "InProgress",
    Complete = "Complete",
}

export const enum RoundPhase {
    Bidding = "Bidding",
    BiddingCompleted = "BiddingCompleted",
    TrickTaking = "TrickTaking",
    TrickCompleted = "TrickCompleted",
}

export type PlayerId = string
