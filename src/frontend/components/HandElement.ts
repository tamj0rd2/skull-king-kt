import {DisconnectGameEventListener, MessageToClient, listenToGameEvents, MessageFromClient} from "../GameEvents";
import {Card} from "../Constants";

export class HandElement extends HTMLElement {
    disconnectFn?: DisconnectGameEventListener

    constructor() {
        super()
    }

    connectedCallback() {
        this.disconnectedCallback()
        this.disconnectFn = listenToGameEvents({
            [MessageToClient.GameStarted]: this.showHand,
            [MessageToClient.RoundStarted]: ({ cardsDealt }) => this.initialiseHand(cardsDealt),
            [MessageToClient.YourTurn]: ({ cards }) => this.makeCardsPlayable(cards),
            [MessageToClient.TrickCompleted]: () => this.makeCardsUnplayable(),
        })
    }

    showHand = () => {
        this.replaceChildren()
        this.innerHTML = `
                <section id="handArea">
                    <h3>Hand</h3>
                    <ul id="hand"></ul>
                </section> 
            `
    }

    initialiseHand = (cards: Card[]) => {
        const hand = this.querySelector("#hand")!!
        cards.forEach(card => {
            const li = document.createElement("li")
            li.innerText = card.name
            li.setAttribute("data-cardType", card.type)
            li.setAttribute("data-cardName", card.name)
            li.setAttribute("suit", card.suit)
            card.number && li.setAttribute("number", card.number.toString())
            hand.appendChild(li)
        })
    }

    makeCardsUnplayable = () => {
        this.querySelectorAll("li button").forEach(button => {
            button.parentNode!!.removeChild(button)
        })
    }

    makeCardsPlayable = (cardNamesToPlayability: Record<string, boolean> ) => {
        this.querySelectorAll("li").forEach(li => {
            const cardName = li.getAttribute("data-cardName")!!
            const isPlayable = cardNamesToPlayability[cardName]
            if (!isPlayable) return

            const button = document.createElement("button")
            button.innerText = "Play"
            button.onclick = () => {
                li.remove()
                this.makeCardsUnplayable()
                socket.send(JSON.stringify({
                    type: MessageFromClient.CardPlayed,
                    cardName: cardName,
                }))
            }
            li.appendChild(button)
        })
    }

    disconnectedCallback() {
        if (this.disconnectFn) this.disconnectFn()
        this.disconnectFn = undefined
    }
}

customElements.define("sk-hand", HandElement)
