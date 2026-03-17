object System {

    fun <T> arraycopy(src: Array<T?>?, srcPos: Int, dest: Array<T?>, destPos: Int, length: Int) {
        src?.copyInto(dest, destPos, srcPos, srcPos + length)
    }

    fun arraycopy(src: ByteArray?, srcPos: Int, dest: ByteArray, destPos: Int, length: Int) {
        src?.copyInto(dest, destPos, srcPos, srcPos + length)
    }

    fun arraycopy(src: IntArray?, srcPos: Int, dest: IntArray, destPos: Int, length: Int) {
        src?.copyInto(dest, destPos, srcPos, srcPos + length)
    }


}
