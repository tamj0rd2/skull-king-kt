import {get, type Readable, readonly, writable} from 'svelte/store'
import type {PlayerId} from "./constants";

declare global {
    const INITIAL_STATE: {
        endpoint: string
        ackTimeoutMs: number
    }
}

type MessageId = string

interface Message {
    id: MessageId
    type: MessageType
    [key: string]: any
    notifications?: ReadonlyArray<Notification>
}

enum MessageType {
    AckFromClient = "Message$Ack$FromClient",
    AckFromServer = "Message$Ack$FromServer",
    Nack = "Message$Nack",
    ToClient = "Message$ToClient",
    ToServer = "Message$ToServer",
    KeepAlive = "Message$KeepAlive"
}

interface Command {
    type: CommandType
    actor: PlayerId
    [key: string]: any
}

export enum CommandType {
    PlaceBid = "Command$PlayerCommand$PlaceBid",
    PlayCard = "Command$PlayerCommand$PlayCard",
    JoinGame = "Command$PlayerCommand$JoinGame"
}

interface Notification {
    type: NotificationType
    players?: PlayerId[]
    playerId?: PlayerId
    [key: string]: any
}

export enum NotificationType {
    PlayerJoined = "Notification$PlayerJoined",
    GameStarted = "Notification$GameStarted",
    RoundStarted = "Notification$RoundStarted",
    BidPlaced = "Notification$BidPlaced",
    BiddingCompleted = "Notification$BiddingCompleted",
    CardPlayed = "Notification$CardPlayed",
    TrickCompleted = "Notification$TrickCompleted",
    TrickStarted = "Notification$TrickStarted",
    GameCompleted = "Notification$GameCompleted",
    YourTurn = "Notification$YourTurn",
    RoundCompleted = "Notification$RoundCompleted",
    YouJoined = "Notification$YouJoined",
}

interface MessageStore extends Readable<ReadonlyArray<Notification>> {
    send: (command: Command) => Promise<void>
}

const playerIdRw = writable<PlayerId>(undefined)
export const playerId = readonly(playerIdRw)

const waitingForServerResponseRW = writable(false)
export const waitingForServerResponse = readonly(waitingForServerResponseRW)

function createMessageStore(): MessageStore {
    let socket: WebSocket
    const knownNotificationTypes = Object.values(NotificationType)

    const {subscribe, update} = writable<ReadonlyArray<Notification>>(undefined, (set) => {
        socket = new WebSocket(INITIAL_STATE.endpoint)
        set([])
        socket.addEventListener("close", (event) => console.warn(`disconnected from ws - ${event.reason}`, event))
        socket.addEventListener("error", (event) => console.error(event))
        socket.addEventListener("message", (event) => {
            try {
                const message = JSON.parse(event.data) as Message
                switch (message.type) {
                    case MessageType.ToClient:
                        break
                    // acks and nacks are handled as part of `sendCommand`
                    case MessageType.AckFromServer:
                    case MessageType.Nack:
                    // keep alive messages don't need handling
                    case MessageType.KeepAlive:
                        return
                    default:
                        throw Error(`unhandled message type - ${message.type}`)
                }
                logInfo(`received ${event.data}`, message)

                const firstUnknownNotification = message.notifications!!.find((n) => !knownNotificationTypes.includes(n.type))
                if (!!firstUnknownNotification) throw Error(`Unknown message type from server: ${firstUnknownNotification.type}`)

                update((notifications) => [...notifications, ...(message.notifications ?? [])])
                // TODO: how can I make it so that I can process the message before sending the ack? Is it even a problem?
                // if it is, one dumb solution could be to delay the below line of code for 500ms so that some time is given to update the UI.
                // but I should find out if there's a better way to do it. Magic numbers bad.
                sendMessage({ type: MessageType.AckFromClient, id: message.id })
            } catch(e) {
                socket.close(4000, (e as Error).message)
                throw e
            }
        })

        return () => socket.close()
    })

    function sendMessage(message: Message) {
        const json = JSON.stringify(message)
        logInfo(`sending ${json}`, message)
        socket.send(json)
    }

    async function sendCommand(command: Command) {
        try {
            if (command.type === CommandType.JoinGame) playerIdRw.set(command.actor)

            waitingForServerResponseRW.set(true)
            let messageId = crypto.randomUUID()

            const signal = AbortSignal.timeout(INITIAL_STATE.ackTimeoutMs)

            socket.addEventListener("message", function handler(event) {
                if (signal.aborted) {
                    this.removeEventListener("message", handler)
                    throw new Error(`${command.type} timed out`)
                }

                const message = JSON.parse(event.data) as Message
                if (message.id !== messageId) return

                this.removeEventListener("message", handler)

                if (message.type === MessageType.Nack) throw new NackError(message.reason)
                if (message.type !== MessageType.AckFromServer) throw new Error(`invalid message type ${message.type}`)

                logInfo(`received ${event.data}`, message)
                update((notifications) => [...notifications, ...(message.notifications ?? [])])
            })
            sendMessage({id: messageId, type: MessageType.ToServer, command})
        } finally {
            waitingForServerResponseRW.set(false)
        }
    }

    return {
        subscribe,
        send: sendCommand,
    }
}

export const messageStore = createMessageStore()

export class NackError extends Error {
    public readonly reason: string

    constructor(reason: string) {
        super(reason)
        this.reason = reason
    }
}

export function logInfo(message: string, extra: any = undefined) {
    const date = new Date()
    const timestamp = `${date.getHours()}:${date.getMinutes()}:${date.getSeconds()}.${date.getMilliseconds()}xxx`
    console.log(`${timestamp} INFO  wsClient:${get(playerId) ?? "unidentified"} -- ${message}`, extra)
}
