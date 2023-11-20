import {get, type Readable, readonly, writable} from 'svelte/store'
import {PlayerCommand, Notification, Message} from "./generated_types";

declare global {
    const INITIAL_STATE: {
        endpoint: string
        ackTimeoutMs: number
    }
}

interface MessageStore extends Readable<ReadonlyArray<Notification>> {
    send: (command: PlayerCommand) => Promise<void>
}

const playerIdRw = writable<string>(undefined)
export const playerId = readonly(playerIdRw)

const waitingForServerResponseRW = writable(false)
export const waitingForServerResponse = readonly(waitingForServerResponseRW)

function createMessageStore(): MessageStore {
    let socket: WebSocket
    const knownNotificationTypes = Object.values(Notification.Type)

    const {subscribe, update} = writable<ReadonlyArray<Notification>>(undefined, (set) => {
        socket = new WebSocket(INITIAL_STATE.endpoint)
        set([])
        socket.addEventListener("close", (event) => console.warn(`disconnected from ws - ${event.reason}`, event))
        socket.addEventListener("error", (event) => console.error(event))
        socket.addEventListener("message", (event) => {
            try {
                const message = JSON.parse(event.data) as Message

                logInfo(`message type: ${message.type}`)
                switch (message.type) {
                    case Message.Type.ToClient:
                        break
                    // acks and nacks are handled as part of `sendCommand`
                    case Message.Type.AckFromServer:
                    case Message.Type.Nack:
                    // keep alive messages don't need handling
                    case Message.Type.KeepAlive:
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
                sendMessage({ type: Message.Type.AckFromClient, id: message.id })
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

    async function sendCommand(command: PlayerCommand) {
        try {
            if (command.type === PlayerCommand.Type.JoinGame) playerIdRw.set(command.actor)

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

                switch (message.type) {
                    case Message.Type.AckFromServer:
                        logInfo(`received ${event.data}`, message)
                        update((notifications) => [...notifications, ...(message.notifications ?? [])])
                        break
                    case Message.Type.Nack:
                        throw new NackError(message.reason)
                    default:
                        throw new Error(`invalid message type ${message.type}`)
                }
            })
            sendMessage({id: messageId, type: Message.Type.ToServer, command})
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
