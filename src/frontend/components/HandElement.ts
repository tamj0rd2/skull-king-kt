import {DisconnectGameEventListener, EventType, listenToGameEvents} from "../GameEvents";
import {Card} from "../Constants";

export class HandElement extends HTMLElement {
    disconnectFn?: DisconnectGameEventListener
    constructor() {
        super()
    }

    connectedCallback() {
        this.disconnectedCallback()
        this.disconnectFn = listenToGameEvents({
            [EventType.GameStarted]: this.showHand,
            [EventType.RoundStarted]: ({ cardsDealt }) => this.initialiseHand(cardsDealt),
            [EventType.TrickStarted]: this.makeCardsPlayable,
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

    makeCardsPlayable = () => {
        this.querySelectorAll("li").forEach(li => {
            const button = document.createElement("button")
            button.innerText = "Play"
            button.onclick = function playCard() {
                let cardName = li.getAttribute("data-cardName")
                li.remove()
                socket.send(JSON.stringify({
                    type: "ClientMessage$CardPlayed",
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
