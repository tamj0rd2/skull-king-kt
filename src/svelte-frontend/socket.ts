import {type Readable, readonly, writable} from 'svelte/store'
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
    ToServer = "Message$ToServer"
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

const waitingForServerResponseRW = writable(false)
export const waitingForServerResponse = readonly(waitingForServerResponseRW)

function createMessageStore(): MessageStore {
    let socket: WebSocket

    const {subscribe, update} = writable<ReadonlyArray<Notification>>(undefined, (set) => {
        console.log({INITIAL_STATE})
        socket = new WebSocket(INITIAL_STATE.endpoint)
        set([])
        return () => socket.close()
    })

    function sendMessage(message: Message) {
        const json = JSON.stringify(message)
        console.log(`SOMEONE is sending ${json}`, message)
        socket.send(json)
    }

    async function sendCommand(command: Command) {
        try {
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

                console.log("received response", message)

                update((notifications) => [...notifications, ...(message.notifications ?? [])])
                console.log("done processing")
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
