import {derived} from "svelte/store";
import {GameState, type PlayerId} from "./constants";
import {messageStore, NotificationType} from "./socket";

// TODO: with every new message, each of these stores need to recompute their values which is inefficient.
// probably not a big deal though since there aren't _that_ many notifications that are received.
export const playerId = derived<typeof messageStore, PlayerId>(messageStore, (messages, set, update) => {
    const playerId = messages.find((message) => message.type === NotificationType.YouJoined)?.playerId
    if (!!playerId) set(playerId)
})

export const players = derived<typeof messageStore, ReadonlyArray<PlayerId>>(messageStore, (messages, set) => {
    set(messages.reduce<PlayerId[]>((accum, message) => {
        switch (message.type) {
            case NotificationType.PlayerJoined:
                return [...accum, message.playerId]
            case NotificationType.GameStarted:
            case NotificationType.YouJoined:
                return message.players
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
