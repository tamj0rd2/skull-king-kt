import {EventType, listenToGameEvents} from "../GameEvents";
import {GameState} from "../Constants";

export class TrickElement extends HTMLElement {
    constructor() {
        super()
    }

    connectedCallback() {
        this.disconnectedCallback()
        this.disconnectFn = listenToGameEvents({
            [EventType.TrickStarted]: this.initialiseTrick,
            [EventType.CardPlayed]: ({playerId, card}) => this.addCard(playerId, card),
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

    addCard = (playerId, card) => {
        const li = document.createElement("li")
        li.innerText = `${playerId}:${card.name}`
        li.setAttribute("player", playerId)
        li.setAttribute("suit", card.suit)
        card.number && li.setAttribute("number", card.number)
        this.querySelector("#trick").appendChild(li);
    }

    disconnectedCallback() {
        if (this.disconnectFn) this.disconnectFn()
        this.disconnectFn = undefined
    }
}
