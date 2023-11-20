declare global {
    const INITIAL_STATE: {
        endpoint: string
        ackTimeoutMs: number
    }
}

export type PlayerId = string
