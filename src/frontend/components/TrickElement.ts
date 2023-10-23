import {NotificationType, listenToNotifications} from "../GameEvents";
import {Card, PlayerId} from "../Constants";
import {DisconnectGameEventListener} from "../Socket";

export class TrickElement extends HTMLElement {
    disconnectFn?: DisconnectGameEventListener

    constructor() {
        super()
    }

    connectedCallback() {
        this.disconnectedCallback()
        this.disconnectFn = listenToNotifications({
            [NotificationType.TrickStarted]: this.initialiseTrick,
            [NotificationType.CardPlayed]: ({playerId, card}) => this.addCard(playerId, card),
            [NotificationType.RoundStarted]: () => this.replaceChildren(),
        })
    }

    initialiseTrick = () => {
        this.replaceChildren()
        this.innerHTML = `
                <section id="trickArea">
                    <h3>Cards in trick</h3>
                    <ul id="trick"></ul>
                </section>            
            `
    }

    addCard = (playerId: PlayerId, card: Card) => {
        const li = document.createElement("li")
        li.innerText = `${playerId}:${card.name}`
        li.setAttribute("player", playerId)
        li.setAttribute("suit", card.suit)
        card.number && li.setAttribute("number", card.number.toString())
        this.querySelector("#trick")!!.appendChild(li);
    }

    disconnectedCallback() {
        if (this.disconnectFn) this.disconnectFn()
        this.disconnectFn = undefined
    }
}

customElements.define("sk-trick", TrickElement)
