import {
    DisconnectGameEventListener,
    Message,
    MessageType,
    Notification,
    NotificationListeners, registerNotificationListeners,
    removeNotificationListeners,
    socket
} from "./Socket";

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

// const knownNotificationTypes = Object.values(NotificationType)
// TODO: work my way through these one by one in whatever order makes sense, then remove this whole thing, prefering the above
const knownNotificationTypes = [
    NotificationType.PlayerJoined,
    NotificationType.GameStarted,
    // NotificationType.RoundStarted,
    // NotificationType.BidPlaced,
    // NotificationType.BiddingCompleted,
    // NotificationType.CardPlayed,
    // NotificationType.TrickCompleted,
    // NotificationType.TrickStarted,
    // NotificationType.GameCompleted,
    // NotificationType.YourTurn,
    // NotificationType.RoundCompleted,
    NotificationType.YouJoined
]

export enum CommandType {
    PlaceBid = "Command$PlayerCommand$BidPlaced",
    PlayCard = "Command$PlayerCommand$CardPlayed",
    // UnhandledServerMessage = "MessageFromClient$UnhandledServerMessage",
    // Error = "MessageFromClient$Error",
    JoinGame = "Command$PlayerCommand$JoinGame"
}

export interface Command {
    type: CommandType
    [key: string]: any
}

export function listenToNotifications(notificationListeners: NotificationListeners): DisconnectGameEventListener {
    function readNotificationsFrom(message: Message): Notification[] {
        switch (message.type) {
            case MessageType.ToClient:
                return message.notifications
            case MessageType.AckFromServer:
                return []
            case MessageType.Nack:
                throw Error("handle nacks from server")
            case MessageType.AckFromClient:
            case MessageType.ToServer:
                throw Error("client shouldn't receive this kind of message")
        }
    }

    function listener(this: WebSocket, event: MessageEvent<any>) {
        try {
            const notifications = readNotificationsFrom(JSON.parse(event.data))

            notifications.forEach((message) => {
                const callback = notificationListeners[message.type]
                if (callback) return callback(message)

                if (!knownNotificationTypes.includes(message.type)) {
                    socket.send(JSON.stringify({
                        type: "UnknownMessageFromServer",
                        offender: message.type,
                    }))
                    console.error(`Unknown message from server: ${message.type}`)
                    socket.close(4000, `Unknown message from server: ${message.type}`)
                }
            })
        } catch (e) {
            socket.send(JSON.stringify({
                stackTrace: (e as Error).stack,
                type: "ClientError",
            }))
            throw e
        }
    }

    const listenerId = crypto.randomUUID()
    registerNotificationListeners(listenerId, notificationListeners)
    socket.addEventListener("message", listener)

    return () => {
        removeNotificationListeners(listenerId)
        socket.removeEventListener("message", listener)
    }
}
