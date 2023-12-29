<script lang="ts">
    import {commander, state} from "./socket";

    let playerIdInput = ''

    function sayHello() {
        $commander.joinGame(playerIdInput)
    }
</script>

{#if !$state}
    <h1>Game Page</h1>
{:else}
    <h1>Game Page - {$state.playerId}</h1>
    <ul id="players">
        {#each $state.playersInRoom as player}
            <li>{player}</li>
        {/each}
    </ul>
    <p id="gameState">{$state.gameState}</p>
{/if}

<form id="joinGame" on:submit|preventDefault={sayHello}>
    <input type="text" name="playerId" bind:value={playerIdInput} />
    <input type="submit" value="Join game" />
</form>

