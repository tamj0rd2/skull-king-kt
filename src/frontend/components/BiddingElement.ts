import {DisconnectGameEventListener, GameEvent, EventType, listenToGameEvents} from "../GameEvents";

export class BiddingElement extends HTMLElement {
    disconnectFn?: DisconnectGameEventListener

    constructor() {
        super()
    }

    connectedCallback() {
        this.disconnectedCallback()
        this.disconnectFn = listenToGameEvents({
            [EventType.RoundStarted]: this.showForm,
            [EventType.BiddingCompleted]: this.hideForm,
        })
    }

    showForm = ({roundNumber}: GameEvent) => {
        this.replaceChildren()
        this.innerHTML = `
            <label>Bid <input type="number" name="bid" min="0" max="${roundNumber}"></label>
            <button id="placeBid" type="button" onclick="onBidSubmit()" disabled>Place Bid</button>
            <p id="biddingError"></p>
        `

        const placeBidBtn = this.querySelector("#placeBid") as HTMLButtonElement;
        const bidInput = this.querySelector(`input[name="bid"]`) as HTMLInputElement;
        const biddingError = this.querySelector("#biddingError") as HTMLParagraphElement;

        bidInput.oninput = (e) => {
            const bid = bidInput.value.replace(/[^0-9]/g, '')
            bidInput.value = bid.toString()
            // @ts-ignore
            if (bid >= 0 && bid <= roundNumber) {
                placeBidBtn.disabled = false
                biddingError.innerText = ""
                return
            }

            placeBidBtn.disabled = true
            biddingError.innerText = `Bid must be between 0 and ${roundNumber}`
        }

        placeBidBtn.onclick = () => {
            socket.send(JSON.stringify({
                type: "ClientMessage$BidPlaced",
                bid: bidInput.value,
            }))
            this.hideForm()
        }
    }

    hideForm = () => this.replaceChildren()

    disconnectedCallback() {
        if (this.disconnectFn) this.disconnectFn()
        this.disconnectFn = undefined
    }
}

customElements.define("sk-biddingform", BiddingElement)
