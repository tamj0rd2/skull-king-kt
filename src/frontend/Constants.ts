declare global {
    var INITIAL_STATE: {
        playerId: PlayerId;
        players: PlayerIds
        waitingForMorePlayers: boolean
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

export interface Card {
    suit: string
    number?: number
    name: string
}

export type PlayerId = string
export type PlayerIds = PlayerId[]
export type ActualBids = {[playerId: PlayerId]: number}
