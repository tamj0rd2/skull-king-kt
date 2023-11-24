<script lang="ts">
    import {waitingForServerResponse, playerId} from "./socket";
    import {bids, BidState, currentPlayer, type DisplayBid, gameState, players, roundPhase} from "./GameState";
    import {RoundPhase, GameState} from "./generated_types";
    import Spinner from "./components/Spinner.svelte";
    import JoinGame from "./components/JoinGame.svelte";
    import Debug from "./components/Debug.svelte";
    import PlaceBid from "./components/PlaceBid.svelte";

    const gameStateAsText: Record<GameState, string> = {
        Complete: "Game over!",
        InProgress: "",
        WaitingForMorePlayers: "Waiting for more players...",
        WaitingToStart: "Waiting to start..."
    }

    const roundPhaseAsText: Record<RoundPhase, string> = {
        Bidding: "Place your bid",
        BiddingCompleted: "All bids are in!",
        TrickTaking: "",
        TrickCompleted: ""
    }

    function bidText(playerId: string, displayBid: DisplayBid): string {
        switch (displayBid.bidState) {
            case BidState.None:
                return `${playerId}`
            case BidState.Hidden:
                return `${playerId}:has bid`
            case BidState.Placed:
                return `${playerId}:${displayBid.bid}`
        }
    }

    $: hasJoinedGame = !!$playerId
</script>

{#if $waitingForServerResponse}
    <Spinner/>
{/if}

<h1>{$playerId ? `Game Page - ${$playerId}` : "Game Page"}</h1>

{#if hasJoinedGame}
    <h2 id="gameState" data-state={$gameState}>{gameStateAsText[$gameState]}</h2>

    {#if $roundPhase}
        <h2 id="roundPhase" data-phase={$roundPhase}>{roundPhaseAsText[$roundPhase]}</h2>

        {#if $roundPhase === RoundPhase.Bidding}
            <PlaceBid />
        {/if}

        {#if $currentPlayer}
            <h3 id="currentPlayer" data-playerId={$currentPlayer}>{$currentPlayer}</h3>
        {/if}

        <ul id="bids">
            {#each Object.entries($bids) as [playerId, displayBid]}
                <li>{bidText(playerId, displayBid)}</li>
            {/each}
        </ul>

    {:else}
        <h3>Players:</h3>
        <ul id="players">
            {#each $players as player}
                <li>{player}</li>
            {/each}
        </ul>
    {/if}
{:else}
    <JoinGame/>
{/if}

<hr/>
<Debug/>