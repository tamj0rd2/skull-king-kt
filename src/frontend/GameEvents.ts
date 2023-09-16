export enum EventType {
    PlayerJoined = "GameEvent$PlayerJoined",
    GameStarted = "GameEvent$GameStarted",
    RoundStarted = "GameEvent$RoundStarted",
    BidPlaced = "GameEvent$BidPlaced",
    BiddingCompleted = "GameEvent$BiddingCompleted",
    CardPlayed = "GameEvent$CardPlayed",
    TrickCompleted = "GameEvent$TrickCompleted",
    TrickStarted = "GameEvent$TrickStarted",
    GameCompleted = "GameEvent$GameCompleted",
}

const knownEventTypes = Object.values(EventType)

export interface GameEvent {
    type: EventType
    [key: string]: any
}

export type GameEventListener = (event: GameEvent) => void
export type GameEventListeners = { [key in EventType]?: GameEventListener }
export type DisconnectGameEventListener = () => void

export function listenToGameEvents(gameEventCallbacks: GameEventListeners): DisconnectGameEventListener {
    function listener(this: WebSocket, event: MessageEvent<any>) {
        try {
            const data = JSON.parse(event.data) as GameEvent
            const callback = gameEventCallbacks[data.type]
            if (callback) return callback(data)

            if (!knownEventTypes.includes(data.type)) {
                socket.send(JSON.stringify({
                    type: "ClientMessage$UnhandledMessageFromServer",
                    offender: data.type,
                }))
                console.error(`Unknown message from server: ${data.type}`)
            }
        } catch (e) {
            socket.send(JSON.stringify({
                stackTrace: (e as Error).stack,
                type: "ClientMessage$Error",
            }))
            throw e
        }
    }

    socket.addEventListener("message", listener)
    return () => socket.removeEventListener("message", listener)
}
