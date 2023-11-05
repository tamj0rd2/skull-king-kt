import {derived} from "svelte/store";
import {GameState, type PlayerId, RoundPhase} from "./constants";
import {logInfo, messageStore, NotificationType} from "./socket";

// TODO: with every new message, each of these stores need to recompute their values which is inefficient.
// probably not a big deal though since there aren't _that_ many notifications that are received.

export const players = derived<typeof messageStore, PlayerId[]>(messageStore, (messages, set) => {
    set(messages.reduce<PlayerId[]>((accum, message) => {
        switch (message.type) {
            case NotificationType.PlayerJoined:
                return [...accum, message.playerId!!]
            case NotificationType.GameStarted:
            case NotificationType.YouJoined:
                return message.players!!
            default:
                return accum
        }
    }, []))
})

export const gameState = derived<typeof messageStore, GameState>(messageStore, (messages, set) => {
    set(messages.reduce<GameState>((accum, message) => {
        switch (message.type) {
            case NotificationType.YouJoined:
                if (message.waitingForMorePlayers) return GameState.WaitingForMorePlayers
                return GameState.WaitingToStart
            default:
                return accum
        }
    }, GameState.WaitingForMorePlayers))
})

export const roundPhase = derived<typeof messageStore, RoundPhase | undefined>(messageStore, (messages, set) => {
    set(messages.reduce<RoundPhase | undefined>((accum, message) => {
        switch (message.type) {
            case NotificationType.RoundStarted:
                return RoundPhase.Bidding
            case NotificationType.BiddingCompleted:
                return RoundPhase.BiddingCompleted
            case NotificationType.TrickStarted:
                return RoundPhase.TrickTaking
            case NotificationType.TrickCompleted:
                return RoundPhase.TrickCompleted
            default:
                return accum
        }
    }, undefined))
})

export const roundNumber = derived<typeof messageStore, PlayerId>(messageStore, (messages, set) => {
    set(messages.slice().reverse().find(m => m.type === NotificationType.RoundStarted)?.roundNumber)
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
type Bids = Record<PlayerId, DisplayBid>

export const bids = derived<typeof messageStore, Bids>(messageStore, (messages, set) => {
    let updatedBids = messages.reduce<Bids>((accum, message) => {
        switch (message.type) {
            case NotificationType.GameStarted:
                return message.players!!.reduce<Bids>((accum, playerId) =>
                    ({...accum, [playerId]: {bidState: BidState.None, bid: undefined}}), {})
            case NotificationType.BidPlaced:
                if (!message.playerId) throw new Error("bid placed notification missing playerId")
                return {...accum, [message.playerId]: {bidState: BidState.Hidden, bid: undefined}}
            default:
                return accum
        }
    }, {});
    set(updatedBids)
    logInfo(`recomputed bids to ${JSON.stringify(updatedBids)}`, updatedBids)
})

