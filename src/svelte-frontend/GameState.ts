import {derived} from "svelte/store";
import {GameState, RoundPhase} from "./generated_types";
import {Notification} from "./generated_types";
import {messageStore} from "./socket";

// TODO: with every new message, each of these stores need to recompute their values which is inefficient.
// probably not a big deal though since there aren't _that_ many notifications that are received.

export const players = derived<typeof messageStore, string[]>(messageStore, (messages, set) => {
    set(messages.reduce<string[]>((accum, message) => {
        switch (message.type) {
            case Notification.Type.PlayerJoined:
                return [...accum, message.playerId!!]
            case Notification.Type.GameStarted:
            case Notification.Type.YouJoined:
                return message.players!!
            default:
                return accum
        }
    }, []))
})

export const gameState = derived<typeof messageStore, GameState>(messageStore, (messages, set) => {
    for (const message of messages) {
        if (message.type === Notification.Type.YouJoined) {
            set(message.waitingForMorePlayers ? GameState.WaitingForMorePlayers : GameState.WaitingToStart)
            return
        }
    }
}, GameState.WaitingForMorePlayers)

export const roundPhase = derived<typeof messageStore, RoundPhase | undefined>(messageStore, (messages, set) => {
    set(messages.reduce<RoundPhase | undefined>((accum, message) => {
        switch (message.type) {
            case Notification.Type.RoundStarted:
                return RoundPhase.Bidding
            case Notification.Type.BiddingCompleted:
                return RoundPhase.BiddingCompleted
            case Notification.Type.TrickStarted:
                return RoundPhase.TrickTaking
            case Notification.Type.TrickCompleted:
                return RoundPhase.TrickCompleted
            default:
                return accum
        }
    }, undefined))
})

export const roundNumber = derived<typeof messageStore, number | undefined>(messageStore, (messages, set) => {
    for (const message of messages) {
        if (message.type === Notification.Type.RoundStarted) {
            set(message.roundNumber)
        }
    }
})

export enum BidState {
    None = "None",
    Hidden = "Hidden",
    Placed = "Placed",
}

export type DisplayBid = {
    bidState: BidState
    bid: number | undefined
}
type Bids = Record<string, DisplayBid>

export const bids = derived<typeof messageStore, Bids>(messageStore, (messages, set) => {
    set(messages.reduce<Bids>((accum, message) => {
        switch (message.type) {
            case Notification.Type.GameStarted:
                return message.players!!.reduce<Bids>((accum, playerId) =>
                    ({...accum, [playerId]: {bidState: BidState.None, bid: undefined}}), {})
            case Notification.Type.BidPlaced:
                if (!message.playerId) throw new Error("bid placed notification missing playerId")
                return {...accum, [message.playerId]: {bidState: BidState.Hidden, bid: undefined}}
            default:
                return accum
        }
    }, {}))
})
