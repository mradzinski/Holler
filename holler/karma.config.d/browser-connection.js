// The tests that draw a message advance a virtual clock by many seconds, frame by frame, and the browser
// can answer nothing while one of those advances runs. On a slow machine (a CI runner draws in software,
// with several browsers at once) that takes longer than Karma waits for a sign of life, and Karma then
// gives the browser up: "Disconnected ... reconnect failed before timeout of 2000ms (ping timeout)".
// These limits give the browser the time it needs. The values are Karma's defaults raised to a minute or two.
config.set({
    // How long the browser may stay silent before its connection counts as lost. Default: 5 seconds.
    pingTimeout: 60000,

    // How long a browser that lost its connection has to come back. Default: 2 seconds.
    browserDisconnectTimeout: 60000,

    // How many lost connections are forgiven before the run fails. Default: none.
    browserDisconnectTolerance: 1,

    // How long Karma waits for any message from the browser. Default: 30 seconds.
    browserNoActivityTimeout: 120000,
});
