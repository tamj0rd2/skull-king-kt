import {DisconnectGameEventListener, listenToNotifications} from "../Socket";
import {Notification, PlayerId} from "../generated_types";

export class PlayersElement extends HTMLElement {
    disconnectFn?: DisconnectGameEventListener
    playersElement?: HTMLUListElement

    constructor() {
        super()
    }

    connectedCallback() {
        this.disconnectedCallback()
        this.disconnectFn = listenToNotifications({
            [Notification.Type.YouJoined]: ({ players }) => players.forEach(this.addPlayer),
            [Notification.Type.PlayerJoined]: ({ playerId }) => this.addPlayer(playerId),
            [Notification.Type.GameStarted]: () => this.parentNode!!.removeChild(this),
        })

        this.innerHTML = `
                <h3>Players</h3>
                <ul id="players"></ul>
            `

        this.playersElement = document.querySelector("#players") as HTMLUListElement
    }

    addPlayer = (playerId: PlayerId) => {
        const li = document.createElement("li")
        li.innerText = playerId
        this.playersElement!!.appendChild(li)
    }

    disconnectedCallback() {
        if (this.disconnectFn) this.disconnectFn()
        this.disconnectFn = undefined
    }
}

customElements.define("sk-players", PlayersElement)
