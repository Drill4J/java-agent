package com.epam.drill.request

actual object RequestHolder {
    actual fun init(isAsync: Boolean) {
        RequestHolderStub.init(isAsync)
    }

    actual fun store(drillRequest: ByteArray) {
        RequestHolderStub.store(drillRequest)
    }

    actual fun dump(): ByteArray? {
        return RequestHolderStub.dump()
    }

    actual fun closeSession() {
        RequestHolderStub.closeSession()
    }
}
