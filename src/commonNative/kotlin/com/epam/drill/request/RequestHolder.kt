package com.epam.drill.request

actual object RequestHolder {
    actual fun store(drillRequest: ByteArray) {
        RequestHolderStub.store(drillRequest)
    }

    actual fun dump(): ByteArray? {
        return RequestHolderStub.dump()
    }

    actual fun closeSession() {
        RequestHolderStub.closeSession()
    }

    actual fun setAsyncMode(isAsync: Boolean) {
        RequestHolderStub.setAsyncMode(isAsync)
    }

}
