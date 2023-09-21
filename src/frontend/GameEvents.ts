export enum MessageToClient {
    PlayerJoined = "MessageToClient$PlayerJoined",
    GameStarted = "MessageToClient$GameStarted",
    RoundStarted = "MessageToClient$RoundStarted",
    BidPlaced = "MessageToClient$BidPlaced",
    BiddingCompleted = "MessageToClient$BiddingCompleted",
    CardPlayed = "MessageToClient$CardPlayed",
    TrickCompleted = "MessageToClient$TrickCompleted",
    TrickStarted = "MessageToClient$TrickStarted",
    GameCompleted = "MessageToClient$GameCompleted",
}

const knownMessagesToClient = Object.values(MessageToClient)

export enum MessageFromClient {
    BidPlaced = "MessageFromClient$BidPlaced",
    CardPlayed = "MessageFromClient$CardPlayed",
    UnhandledServerMessage = "MessageFromClient$UnhandledServerMessage",
    Error = "MessageFromClient$Error",
}

export interface GameEvent {
    type: MessageToClient
    [key: string]: any
}

export type GameEventListener = (event: GameEvent) => void
export type GameEventListeners = { [key in MessageToClient]?: GameEventListener }
export type DisconnectGameEventListener = () => void

export function listenToGameEvents(gameEventCallbacks: GameEventListeners): DisconnectGameEventListener {
    function listener(this: WebSocket, event: MessageEvent<any>) {
        try {
            const data = JSON.parse(event.data) as GameEvent
            const callback = gameEventCallbacks[data.type]
            if (callback) return callback(data)

            if (!knownMessagesToClient.includes(data.type)) {
                socket.send(JSON.stringify({
                    type: MessageFromClient.UnhandledServerMessage,
                    offender: data.type,
                }))
                console.error(`Unknown message from server: ${data.type}`)
            }
        } catch (e) {
            socket.send(JSON.stringify({
                stackTrace: (e as Error).stack,
                type: MessageFromClient.Error,
            }))
            throw e
        }
    }

    socket.addEventListener("message", listener)
    return () => socket.removeEventListener("message", listener)
}
