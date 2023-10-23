import {Command, knownNotificationTypes, NotificationType, PlayerId} from "./Constants";

const socket = new WebSocket(INITIAL_STATE.endpoint)

socket.addEventListener("close", (event) => console.warn(`disconnected from ws - ${event.reason}`, event))
socket.addEventListener("error", (event) => console.error(event))
socket.addEventListener("message", (event) => {
    console.log(`${getPlayerId()} received ${event.data}`)

    try {
        const message = JSON.parse(event.data) as Message
        switch (message.type) {
            case MessageType.ToClient:
                break
            // acks and nacks are handled as part of `sendCommand`
            case MessageType.AckFromServer:
            case MessageType.Nack:
                return
            default:
                throw Error("unhandled message type")
        }

        message.notifications!!.forEach((notification) => {
            listenerRegistry.forEach((listeners) => {
                if (!knownNotificationTypes.includes(notification.type)) {
                    // TODO: do something about this
                    socket.send(JSON.stringify({
                        type: "UnknownMessageFromServer",
                        offender: notification.type,
                    }))
                    console.error(`Unknown message from server: ${notification.type}`)
                    socket.close(4000, `Unknown message from server: ${notification.type}`)
                    return
                }

                listeners[notification.type]?.(notification)
            })
        })

        acknowledgeMessage(message)
    } catch(e) {
        // TODO: do something about this
        socket.send(JSON.stringify({
            stackTrace: (e as Error).stack,
            type: "ClientError",
        }))
        throw e
    }
})

export { socket }

export enum MessageType {
    AckFromClient = "Message$Ack$FromClient",
    AckFromServer = "Message$Ack$FromServer",
    Nack = "Message$Nack",
    ToClient = "Message$ToClient",
    ToServer = "Message$ToServer"
}

export interface Message {
    type: MessageType
    id: String
    notifications?: Notification[]
    [key: string]: any
}

const spinner = document.querySelector("#spinner")!!

const listenerRegistry = new Map<string, NotificationListeners>()
export function registerNotificationListeners(id: string, listenersToAdd: NotificationListeners) {
    listenerRegistry.set(id, listenersToAdd)
}
export function removeNotificationListeners(id: string) {
    listenerRegistry.delete(id)
}

export function sendCommand(command: Command): Promise<void> {
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

            if (message.type === MessageType.Nack) return reject(new Error("server nacked the command. handle this..."))
            if (message.type !== MessageType.AckFromServer) return reject(new Error(`invalid message type ${message.type}`))

            message.notifications!!.forEach((notification) => {
                listenerRegistry.forEach((listeners) => {
                    listeners[notification.type]?.(notification)
                })
            })

            console.log("done processing")
            resolve()
        })

        send({ id: messageId, type: MessageType.ToServer, command })
    }).finally(() => spinner.classList.add("u-hidden"))
}

export function acknowledgeMessage(messageToAck: Message) {
    send({ type: MessageType.AckFromClient, id: messageToAck.id})
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

export interface Notification {
    type: NotificationType
    [key: string]: any
}

export type NotificationListener = (notification: Notification) => void
export type NotificationListeners = { [key in NotificationType]?: NotificationListener }
export type DisconnectGameEventListener = () => void

export function listenToNotifications(notificationListeners: NotificationListeners): DisconnectGameEventListener {
    const listenerId = crypto.randomUUID()
    registerNotificationListeners(listenerId, notificationListeners)
    return () => removeNotificationListeners(listenerId)
}
