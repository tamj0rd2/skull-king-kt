import {Message, Notification, PlayerCommand, PlayerId} from "../generated_types";

declare global {
    const INITIAL_STATE: {
        endpoint: string
        ackTimeoutMs: number
    }
}

const socket = new WebSocket(INITIAL_STATE.endpoint)

const knownNotificationTypes = Object.values(Notification.Type)

socket.addEventListener("close", (event) => console.warn(`disconnected from ws - ${event.reason}`, event))
socket.addEventListener("error", (event) => console.error(event))
socket.addEventListener("message", (event) => {
    console.log(`${getPlayerId()} received ${event.data}`)

    try {
        const message = JSON.parse(event.data) as Message
        switch (message.type) {
            case Message.Type.ToClient:
                break
            // acks and nacks are handled as part of `sendCommand`
            case Message.Type.AckFromServer:
            case Message.Type.Nack:
            case Message.Type.KeepAlive:
                return
            default:
                throw Error(`unhandled message type - ${message.type}`)
        }

        message.notifications!!.forEach((notification) => {
            listenerRegistry.forEach((listeners) => {
                if (!knownNotificationTypes.includes(notification.type)) {
                    throw Error(`Unknown message from server: ${notification.type}`)
                }

                // TODO: what's the correct way to do this? can't remember
                listeners[notification.type]?.(notification as any)
            })
        })

        acknowledgeMessage(message)
    } catch(e) {
        socket.close(4000, (e as Error).message)
        throw e
    }
})

export { socket }

const spinner = document.querySelector("#spinner")!!

const listenerRegistry = new Map<string, NotificationListeners>()
export function registerNotificationListeners(id: string, listenersToAdd: NotificationListeners) {
    listenerRegistry.set(id, listenersToAdd)
}
export function removeNotificationListeners(id: string) {
    listenerRegistry.delete(id)
}

export function sendCommand(command: PlayerCommand): Promise<void> {
    return new Promise<void>((resolve, reject) => {
        spinner.classList.remove("u-hidden")

        const messageId = crypto.randomUUID()
        const signal = AbortSignal.timeout(INITIAL_STATE.ackTimeoutMs)

        socket.addEventListener("message", function handler(event) {
            if (signal.aborted) {
                this.removeEventListener("message", handler)
                return reject(new Error("timed out"))
            }

            const message = JSON.parse(event.data) as Message
            if (message.id !== messageId) return

            this.removeEventListener("message", handler)

            if (message.type === Message.Type.Nack) return reject(new NackError(message.reason))
            if (message.type !== Message.Type.AckFromServer) return reject(new Error(`invalid message type ${message.type}`))

            message.notifications!!.forEach((notification) => {
                listenerRegistry.forEach((listeners) => {
                    // TODO: what's the correct way to do this? can't remember
                    listeners[notification.type]?.(notification as any)
                })
            })

            console.log("done processing")
            resolve()
        })

        send({ id: messageId, type: Message.Type.ToServer, command })
    }).finally(() => spinner.classList.add("u-hidden"))
}

export function acknowledgeMessage(messageToAck: Message) {
    send({ type: Message.Type.AckFromClient, id: messageToAck.id})
}

function send(message: Message) {
    const json = JSON.stringify(message)
    console.log(`${playerId} is sending ${json}`, message)
    socket.send(json)
}

let playerId: PlayerId = "unidentified"

export function getPlayerId(): PlayerId {
    return playerId
}

export function setPlayerId(newId: PlayerId) {
    playerId = newId
}


export type NotificationListeners = { [key in Notification.Type]?: (notification: Extract<Notification, { type: key }>) => void }
export type DisconnectGameEventListener = () => void

export function listenToNotifications(notificationListeners: NotificationListeners): DisconnectGameEventListener {
    const listenerId = crypto.randomUUID()
    registerNotificationListeners(listenerId, notificationListeners)
    return () => removeNotificationListeners(listenerId)
}

export enum ErrorCode {
    PlayerWithSameNameAlreadyInGame = "PlayerWithSameNameAlreadyInGame",
    BidLessThan0OrGreaterThanRoundNumber = "BidLessThan0OrGreaterThanRoundNumber",
}
const knownErrorCodes = Object.keys(ErrorCode)

export class NackError extends Error {
    public readonly reason: string

    constructor(reason: string) {
        super(reason.toString())
        this.reason = reason
    }
}
