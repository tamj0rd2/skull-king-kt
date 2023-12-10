import {DisconnectGameEventListener, listenToNotifications} from "../Socket";
import {Notification, Card, PlayerId} from "../../generated_types";

export class TrickElement extends HTMLElement {
    disconnectFn?: DisconnectGameEventListener

    constructor() {
        super()
    }

    connectedCallback() {
        this.disconnectedCallback()
        this.disconnectFn = listenToNotifications({
            [Notification.Type.TrickStarted]: this.initialiseTrick,
            [Notification.Type.CardPlayed]: ({playerId, card}) => this.addCard(playerId, card),
            [Notification.Type.RoundStarted]: () => this.replaceChildren(),
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
        if (card.type === Card.Type.NumberedCard) {
            card.number && li.setAttribute("number", card.number.toString())
        }
        this.querySelector("#trick")!!.appendChild(li);
    }

    disconnectedCallback() {
        if (this.disconnectFn) this.disconnectFn()
        this.disconnectFn = undefined
    }
}

customElements.define("sk-trick", TrickElement)
