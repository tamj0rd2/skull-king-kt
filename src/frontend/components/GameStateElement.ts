import {DisconnectGameEventListener, MessageToClient, listenToGameEvents} from "../GameEvents";
import {GameState} from "../Constants";

export class GameStateElement extends HTMLElement {
    disconnectFn?: DisconnectGameEventListener

    constructor() {
        super()
    }

    connectedCallback() {
        this.disconnectedCallback()
        this.disconnectFn = listenToGameEvents({
            [MessageToClient.PlayerJoined]: ({waitingForMorePlayers}) => this.updateBasedOnPlayers(waitingForMorePlayers),
            [MessageToClient.GameStarted]: () => this.updateGameState(GameState.InProgress),
            [MessageToClient.GameCompleted]: () => this.updateGameState(GameState.Complete),
        })

        this.innerHTML = `<h2 id="gameState"></h2>`
        this.updateBasedOnPlayers(INITIAL_STATE.waitingForMorePlayers)
    }

    updateBasedOnPlayers = (waitingForMorePlayers: boolean) => {
        if (waitingForMorePlayers) this.updateGameState(GameState.WaitingForMorePlayers)
        else this.updateGameState(GameState.WaitingToStart)
    }

    updateGameState = (gameState: GameState) => {
        const gameStateEl = this.querySelector("#gameState") as HTMLHeadingElement
        const gameStateMapping = {
            [GameState.WaitingForMorePlayers]: "Waiting for more players...",
            [GameState.WaitingToStart]: "Waiting for the game to start",
            [GameState.InProgress]: "The game has started!",
            [GameState.Complete]: "The game is over!"
        }

        const text = gameStateMapping[gameState]
        if (!text) throw new Error("Unknown game state: " + gameState)
        gameStateEl.innerText = gameStateMapping[gameState]
        gameStateEl.setAttribute("data-state", gameState)
    }

    disconnectedCallback() {
        if (this.disconnectFn) this.disconnectFn()
        this.disconnectFn = undefined
    }
}

customElements.define("sk-gamestate", GameStateElement)
