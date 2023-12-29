import {derived, get, readable, readonly, writable} from "svelte/store";
import {type Bid, Message, PlayerCommand, type PlayerGameState, type PlayerId} from "../generated_types";
import {v4 as uuidv4} from "uuid";

declare global {
    const INITIAL_STATE: {
        endpoint: string
        ackTimeoutMs: number
    }
}

interface WsClient {
    sendCommand: (command: PlayerCommand) => void
    state: PlayerGameState
}

const ws = readable<WebSocket>(
    undefined,
    set => {
        const socket = new WebSocket(INITIAL_STATE.endpoint)

        socket.onopen = () => console.log("connected to ws")
        socket.onclose = () => console.log("disconnected from ws")
        socket.onerror = (event) => {
            console.error("WebSocket error observed:", event)
            socket.close()
        }

        set(socket)
        return () => socket.close()
    }
)

const commandIsInProgressRW = writable(false)
export const commandIsInProgress = readonly(commandIsInProgressRW)

export const commander = derived<typeof ws, Commander>(ws, (ws, set, update) => {
    const doCommand = (command: PlayerCommand) => {
        commandIsInProgressRW.set(true)

        const message: Message.ToServer = {
            command: command,
            id: uuidv4(),
            type: Message.Type.ToServer
        }

        const responseListener = (event: MessageEvent) => {
            const response = JSON.parse(event.data) as Message
            if (response.id !== message.id) return

            if (response.type === Message.Type.Rejection) {
                throw new Error(`command ${command.type} ${response.id} was rejected`)
            }

            if (response.type !== Message.Type.AcceptanceFromServer) return

            console.log(`received acknowledgement for ${response.id}`)
            ws.removeEventListener("message", responseListener)
            commandIsInProgressRW.set(false)
        }

        ws.addEventListener("message", responseListener)

        const json = JSON.stringify(message)
        console.log(`sending command - ${json}`)
        ws.send(json)
    }

    set({
        joinGame: (playerId: PlayerId) => doCommand({actor: playerId, type: PlayerCommand.Type.JoinGame}),
        placeBid: (bid: Bid) => doCommand({actor: get(state).playerId, type: PlayerCommand.Type.PlaceBid, bid: bid})
    })
})

export const state = derived<typeof ws, PlayerGameState>(ws, (ws, set, update) => {
    ws.addEventListener("message", (event) => {
        const message = JSON.parse(event.data) as Message
        switch (message.type) {
            case Message.Type.AcceptanceFromServer:
                set(message.state)
                return
            case Message.Type.Rejection:
                // these are processed by the commander
                return
            case Message.Type.ToClient:
                set(message.state)
                const response: Message.AcceptanceFromClient = {
                    id: message.id,
                    type: Message.Type.AcceptanceFromClient
                }
                console.log(`sending acceptance for ${message.id}`)
                ws.send(JSON.stringify(response))
                return
            case Message.Type.KeepAlive:
                // no processing required
                return
            case Message.Type.AcceptanceFromClient:
            case Message.Type.ToServer:
            default:
                throw new Error(`unexpected message type - ${message.type}`)
        }
    })
})

interface Commander {
    joinGame: (playerId: PlayerId) => void
    placeBid: (bid: Bid) => void
}
