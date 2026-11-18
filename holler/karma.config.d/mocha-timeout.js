// The tests that draw a message advance a virtual clock by many seconds, frame by frame. In a browser
// that takes longer than Mocha's default limit of two seconds per test, so the limit is raised.
config.set({
    client: {
        mocha: {
            timeout: 60000,
        },
    },
});
