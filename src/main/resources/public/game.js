function connectToWs(socket) {
    const EventTypes = {
        PlayerJoined: "GameEvent$PlayerJoined",
        GameStarted: "GameEvent$GameStarted",
        RoundStarted: "GameEvent$RoundStarted",
        BidPlaced: "GameEvent$BidPlaced",
        BiddingCompleted: "GameEvent$BiddingCompleted",
        CardPlayed: "GameEvent$CardPlayed",
        TrickCompleted: "GameEvent$TrickCompleted",
        TrickStarted: "GameEvent$TrickStarted",
        GameCompleted: "GameEvent$GameCompleted",
    }
    const knownEventTypes = Object.values(EventTypes)

    function listenToGameEvents(gameEventsToCallbacks) {
        const listener = (event) => {
            try {
                const data = JSON.parse(event.data)
                const callback = gameEventsToCallbacks[data.type]
                if (callback) return callback(data)

                if (!knownEventTypes.includes(data.type)) {
                    socket.send(JSON.stringify({
                        type: "ClientMessage$UnhandledMessageFromServer",
                        offender: data.type,
                    }))
                    console.error(`Unknown message from server: ${data.type}`)
                }
            } catch (e) {
                socket.send(JSON.stringify({
                    stackTrace: e.stack,
                    type: "ClientMessage$Error",
                }))
                throw e
            }
        }
        socket.addEventListener("message", listener)
        return () => socket.removeEventListener("message", listener)
    }

    configureWs()

    function configureWs() {
        const body = document.querySelector("body")
        const biddingForm = document.createElement("sk-biddingform")
        body.appendChild(biddingForm)
        const bidsEl = document.createElement("sk-bids")
        body.appendChild(bidsEl)
        const gamePhaseEl = document.getElementById("gamePhase")

        function updateGamePhase(gamePhase) {
            const gamePhaseEl = document.getElementById("gamePhase")
            const gamePhaseMapping = {
                "Bidding": "Place your bid!",
                "BiddingCompleted": "Bidding completed :)",
                "TrickTaking": "It's trick taking time!",
                "TrickComplete": "Trick completed :)"
            }

            const text = gamePhaseMapping[gamePhase]
            if (!text) throw new Error("Unknown game phase: " + gamePhase)
            gamePhaseEl.innerText = gamePhaseMapping[gamePhase]
            gamePhaseEl.setAttribute("data-phase", gamePhase)
        }

        socket.addEventListener("close", (event) => {
            console.error("disconnected from ws")
        })

        socket.addEventListener("message", (event) => {
            try {
                const data = JSON.parse(event.data)
                switch (data.type) {
                    case EventTypes.PlayerJoined:
                        const players = document.getElementById("players")
                        const li = document.createElement("li")
                        li.innerText = data.playerId
                        players.appendChild(li)

                        // TODO: I don't think I need this anymore? Or I should have a message saying waiting to start
                        if (!data.waitingForMorePlayers) {
                            const gameStateEl = document.getElementById("gameState")
                            gameStateEl.innerText = ""
                        }
                        return
                    case EventTypes.GameStarted:
                        const gameStateEl = document.getElementById("gameState")
                        gameStateEl.innerText = "The game has started :D"
                        return
                    case EventTypes.RoundStarted:
                        const roundNumberEl = document.getElementById("roundNumber")
                        roundNumberEl.innerText = data.roundNumber
                        updateGamePhase("Bidding")

                        const trickNumberEl = document.getElementById("trickNumber")
                        trickNumberEl.innerText = ""

                        const trick = document.getElementById("trick")
                        trick.textContent = ""

                        const handEl = document.getElementById("hand")
                        return data.cardsDealt.forEach(card => {
                            const li = document.createElement("li")
                            li.innerText = card.name
                            li.setAttribute("suit", card.suit)
                            card.number && li.setAttribute("number", card.number)

                            const button = document.createElement("button")
                            button.innerText = "Play"
                            button.onclick = function playCard() {
                                li.remove()
                                socket.send(JSON.stringify({
                                    type: "ClientMessage$CardPlayed",
                                    cardName: card.name,
                                }))
                            }
                            li.appendChild(button)
                            handEl.appendChild(li)
                        });
                    case EventTypes.BidPlaced:
                        return
                    case EventTypes.BiddingCompleted:
                        updateGamePhase("BiddingCompleted")
                        return
                    case EventTypes.CardPlayed:
                        const trick1 = document.getElementById("trick")
                        const li1 = document.createElement("li")
                        li1.innerText = `${data.playerId}:${data.card.name}`
                        li1.setAttribute("player", data.playerId)
                        li1.setAttribute("suit", data.card.suit)
                        data.card.number && li1.setAttribute("number", data.card.number)
                        return trick1.appendChild(li1);
                    case EventTypes.TrickCompleted:
                        return updateGamePhase("TrickComplete");
                    case EventTypes.TrickStarted:
                        updateGamePhase("TrickTaking")
                        const trickNumberEl1 = document.getElementById("trickNumber")
                        trickNumberEl1.innerText = data.trickNumber

                        const trick2 = document.getElementById("trick")
                        return trick2.textContent = "";
                    case EventTypes.GameCompleted:
                        const gameStateEl2 = document.getElementById("gameState")
                        return gameStateEl2.innerText = "The game is over!";
                    default: {
                        socket.send(JSON.stringify({
                            type: "ClientMessage$UnhandledMessageFromServer",
                            offender: data.type,
                        }))
                        console.error(`Unknown message from server: ${data.type}`)
                    }
                }
            } catch (e) {
                socket.send(JSON.stringify({
                    stackTrace: e.stack,
                    type: "ClientMessage$Error",
                }))
                throw e
            }
        });
    }

    class BiddingForm extends HTMLElement {
        constructor() {
            super()
        }

        connectedCallback() {
            this.disconnectedCallback()
            this.disconnectFn = listenToGameEvents({
                [EventTypes.RoundStarted]: this.showForm,
                [EventTypes.BiddingCompleted]: this.hideForm,
            })
        }

        showForm = ({roundNumber}) => {
            this.innerHTML = `
                <label>Bid <input type="number" name="bid" min="0" max="${roundNumber}"></label>
                <button id="placeBid" type="button" onclick="onBidSubmit()" disabled>Place Bid</button>
                <p id="biddingError"></p>
            `

            const placeBidBtn = this.querySelector("#placeBid");
            const bidInput = this.querySelector(`input[name="bid"]`);
            const biddingError = this.querySelector("#biddingError");

            bidInput.oninput = (e) => {
                const bid = e.target.value.replace(/[^0-9]/g, '')
                bidInput.value = bid
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

    class Bids extends HTMLElement {
        constructor() {
            super();
            listenToGameEvents({
                [EventTypes.GameStarted]: ({players}) => this.initialiseForPlayers(players),
                [EventTypes.BidPlaced]: ({playerId}) => this.indicateThatPlayerHasBid(playerId),
                [EventTypes.BiddingCompleted]: ({bids}) => this.showActualBids(bids),
            })
        }

        connectedCallback() {
            this.innerHTML = `
                <section id="bidsArea">
                    <h3>Bids</h3>
                    <ul id="bids"></ul>
                </section>
            `
        }

        initialiseForPlayers = (players) => {
            const bids = this.querySelector("#bids")
            players.forEach(playerId => {
                const li = document.createElement("li")
                li.textContent = playerId
                li.setAttribute("data-playerBid", playerId)
                li.appendChild(document.createElement("span"))
                bids.appendChild(li)
            })
        }

        indicateThatPlayerHasBid = (playerId) => {
            this.querySelector(`[data-playerBid="${playerId}"] span`).innerText = ":" + "has bid"
        }

        showActualBids = (bids) => {
            this.querySelectorAll(`[data-playerBid]`).forEach(el => {
                const playerId = el.getAttribute("data-playerBid")
                const bid = bids[playerId]
                el.querySelector("span").innerText = ":" + bid
            })
        }
    }

    customElements.define("sk-biddingform", BiddingForm)
    customElements.define("sk-bids", Bids)
}
