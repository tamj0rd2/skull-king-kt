import {DisconnectGameEventListener, NotificationType, listenToNotifications} from "../GameEvents";
import {GameState} from "../Constants";

export class GameStateElement extends HTMLElement {
    disconnectFn?: DisconnectGameEventListener

    constructor() {
        super()
    }

    connectedCallback() {
        this.disconnectedCallback()
        this.disconnectFn = listenToNotifications({
            [NotificationType.YouJoined]: ({waitingForMorePlayers}) => this.setBasedOnPlayers(waitingForMorePlayers),
            [NotificationType.PlayerJoined]: ({waitingForMorePlayers}) => this.setBasedOnPlayers(waitingForMorePlayers),
            [NotificationType.GameStarted]: () => this.set(GameState.InProgress),
            [NotificationType.GameCompleted]: () => this.set(GameState.Complete),
        })

        this.innerHTML = `<h2 id="gameState"></h2>`
        // TODO: replace this
        // this.setBasedOnPlayers(INITIAL_STATE.waitingForMorePlayers)
    }

    setBasedOnPlayers = (waitingForMorePlayers: boolean) => {
        const gameState = waitingForMorePlayers
            ? GameState.WaitingForMorePlayers
            : GameState.WaitingToStart

        this.set(gameState)
    }

    set = (gameState: GameState) => {
        const gameStateEl = this.querySelector("#gameState") as HTMLHeadingElement
        const gameStateMapping = {
            [GameState.WaitingForMorePlayers]: "Waiting for more players...",
            [GameState.WaitingToStart]: "Waiting for the game to start",
            [GameState.InProgress]: "",
            [GameState.Complete]: "The game is over!"
        }

        const text = gameStateMapping[gameState]
        if (text === undefined) throw new Error("Unknown game state: " + gameState)
        gameStateEl.innerText = gameStateMapping[gameState]
        gameStateEl.setAttribute("data-state", gameState)
    }

    disconnectedCallback() {
        if (this.disconnectFn) this.disconnectFn()
        this.disconnectFn = undefined
    }
}

customElements.define("sk-gamestate", GameStateElement)
