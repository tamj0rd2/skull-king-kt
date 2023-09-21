import {DisconnectGameEventListener, MessageToClient, listenToGameEvents, MessageFromClient} from "../GameEvents";
import {Card, PlayerId} from "../Constants";

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
            [MessageToClient.TrickStarted]: ({ firstPlayer }) => {
                if (INITIAL_STATE.playerId === firstPlayer) this.makeCardsPlayable()
            },
            [MessageToClient.CardPlayed]: ({ playerId, nextPlayer }) => {
                if (INITIAL_STATE.playerId === playerId) this.makeCardsUnplayable()
                if (INITIAL_STATE.playerId === nextPlayer) this.makeCardsPlayable()
            },
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

    makeCardsPlayable = () => {
        this.querySelectorAll("li").forEach(li => {
            const button = document.createElement("button")
            button.innerText = "Play"
            button.onclick = function playCard() {
                let cardName = li.getAttribute("data-cardName")
                li.remove()
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
