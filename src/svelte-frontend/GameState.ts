import {derived} from "svelte/store";
import {GameState, type PlayerId, RoundPhase} from "./generated_types";
import {Notification} from "./generated_types";
import {messageStore} from "./socket";

// TODO: with every new message, each of these stores need to recompute their values which is inefficient.
// probably not a big deal though since there aren't _that_ many notifications that are received.

export const players = derived<typeof messageStore, PlayerId[]>(messageStore, (messages, set) => {
    set(messages.reduce<PlayerId[]>((accum, message) => {
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
    const message = findLastMessage(messages, Notification.Type.YouJoined)
    if (!!message) set(message.waitingForMorePlayers ? GameState.WaitingForMorePlayers : GameState.WaitingToStart)
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
    const message = findLastMessage(messages, Notification.Type.RoundStarted)
    if (!!message) set(message.roundNumber)
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

export const currentPlayer = derived<typeof messageStore, PlayerId | undefined>(messageStore, (messages, set) => {
    const message = findLastMessage(messages, [Notification.Type.CardPlayed, Notification.Type.TrickStarted])
    switch (message?.type) {
        case Notification.Type.CardPlayed:
            return set(message.nextPlayer ?? undefined)
        case Notification.Type.TrickStarted:
            return set(message.firstPlayer)
    }
})

type NotificationByType<T extends Notification.Type> = Extract<Notification, { type: T }>

function findLastMessage<T extends Notification.Type>(messages: readonly Notification[], type: T): NotificationByType<T> | undefined
function findLastMessage<T extends Notification.Type>(messages: readonly Notification[], type: T[]): NotificationByType<T> | undefined
function findLastMessage<T extends Notification.Type>(messages: readonly Notification[], type: T | T[]): NotificationByType<T> | undefined {
    const targetTypes = new Set<Notification.Type>(Array.isArray(type) ? type : [type])
    return [...messages].reverse().find(message => targetTypes.has(message.type)) as NotificationByType<T> | undefined
}
