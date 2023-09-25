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
    YourTurn = "MessageToClient$YourTurn",
    RoundCompleted = "MessageToClient$RoundCompleted",
    Multi = "MessageToClient$Multi",
}

const knownMessagesToClient = Object.values(MessageToClient).filter((v) => v != MessageToClient.Multi)

export enum MessageFromClient {
    BidPlaced = "MessageFromClient$BidPlaced",
    CardPlayed = "MessageFromClient$CardPlayed",
    UnhandledServerMessage = "MessageFromClient$UnhandledServerMessage",
    Error = "MessageFromClient$Error",
}

// TODO: bad naming
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
            const messages: GameEvent[] = data.type === MessageToClient.Multi ? data.messages : [data]

            messages.forEach((message) => {
                const callback = gameEventCallbacks[message.type]
                if (callback) return callback(message)

                if (!knownMessagesToClient.includes(message.type)) {
                    socket.send(JSON.stringify({
                        type: MessageFromClient.UnhandledServerMessage,
                        offender: message.type,
                    }))
                    console.error(`Unknown message from server: ${message.type}`)
                    socket.close(4000, `Unknown message from server: ${message.type}`)
                }
            })
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
