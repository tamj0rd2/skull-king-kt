import {Command, NotificationType} from "./GameEvents";
import {PlayerId} from "./Constants";

const socket = new WebSocket(INITIAL_STATE.endpoint)

socket.addEventListener("error", (event) => console.error(event))
socket.addEventListener("message", (event) => console.log(`${getPlayerId()} received ${event.data}`, JSON.parse(event.data)))
socket.addEventListener("close", (event) => console.warn(`disconnected from ws - ${event.reason}`, event))
socket.addEventListener("open", () => console.log("connected to ws"))

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
    [key: string]: any
}

const spinner = document.querySelector("#spinner")!!

const listeners = new Map<string, NotificationListeners>()
export function registerNotificationListeners(id: string, listenersToAdd: NotificationListeners) {
    listeners.set(id, listenersToAdd)
}
export function removeNotificationListeners(id: string) {
    listeners.delete(id)
}

export function sendCommand(command: Command): Promise<void> {
    return new Promise<void>((resolve, reject) => {
        spinner.classList.remove("u-hidden")

        const messageId = crypto.randomUUID()
        const signal = AbortSignal.timeout(5000)

        socket.addEventListener("message", function handler(event) {
            if (signal.aborted) {
                this.removeEventListener("message", handler)
                return reject(new Error("timed out"))
            }

            const message = JSON.parse(event.data) as Message
            if (message.id !== messageId) return
            if (message.type === MessageType.Nack) return reject(new Error("server nacked the command. handle this..."))
            if (message.type !== MessageType.AckFromServer) return reject(new Error(`invalid message type ${message.type}`))

            this.removeEventListener("message", handler)

            const notifications: Notification[] = message.notifications
            notifications.forEach((notification) => {
                listeners.forEach((listeners) => {
                    const listener = listeners[notification.type]
                    if (!!listener) listener(notification)
                })
            })

            listeners.forEach((listener) => listener)

            spinner.classList.add("u-hidden")
            resolve()
        })

        const message = { id: messageId, type: MessageType.ToServer, command };
        const json = JSON.stringify(message);
        console.log(`sending ${json}`, message)
        socket.send(json)
    })
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
