package com.anatawa12.modPatching.binary.internal

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.io.ByteArrayInputStream

class UtilKtTest : FunSpec({
    context("readFully") {
        test("small byte array") {
            val bytes = ByteArray(128) { it.toByte() }
            val buffer = ByteArray(1024)
            ByteArrayInputStream(bytes).readFully(buffer) shouldBe bytes.size
            buffer.copyOf(128) shouldBe bytes
        }

        test("long byte array") {
            val bytes = ByteArray(1024 + 128) { it.toByte() }
            val buffer = ByteArray(1024)
            val bais = ByteArrayInputStream(bytes)
            bais.readFully(buffer) shouldBe 1024
            buffer shouldBe bytes.copyOf(1024)

            bais.readFully(buffer) shouldBe 128
            buffer.copyOf(128) shouldBe bytes.copyOfRange(1024, 1024 + 128)
        }
    }

    context("isSameDataStream") {
        test("small same") {
            val bytes = ByteArray(128) { it.toByte() }
            val bais0 = ByteArrayInputStream(bytes)
            val bais1 = ByteArrayInputStream(bytes)
            isSameDataStream(bais0, listOf(bais1)) shouldBe true
        }

        test("long same") {
            val bytes = ByteArray(1024 + 128) { it.toByte() }
            val bais0 = ByteArrayInputStream(bytes)
            val bais1 = ByteArrayInputStream(bytes)
            isSameDataStream(bais0, listOf(bais1)) shouldBe true
        }

        test("small difference") {
            val bytes0 = ByteArray(128) { it.toByte() }
            val bytes1 = ByteArray(128) { it.toByte() }
            bytes1[120] = 11
            val bais0 = ByteArrayInputStream(bytes0)
            val bais1 = ByteArrayInputStream(bytes1)
            isSameDataStream(bais0, listOf(bais1)) shouldBe false
        }

        test("long difference at head") {
            val bytes0 = ByteArray(1024 + 128) { it.toByte() }
            val bytes1 = ByteArray(1024 + 128) { it.toByte() }
            bytes1[120] = 11
            val bais0 = ByteArrayInputStream(bytes0)
            val bais1 = ByteArrayInputStream(bytes1)
            isSameDataStream(bais0, listOf(bais1)) shouldBe false
        }

        test("long difference at last") {
            val bytes0 = ByteArray(1024 + 128) { it.toByte() }
            val bytes1 = ByteArray(1024 + 128) { it.toByte() }
            bytes1[1024 + 120] = 11
            val bais0 = ByteArrayInputStream(bytes0)
            val bais1 = ByteArrayInputStream(bytes1)
            isSameDataStream(bais0, listOf(bais1)) shouldBe false
        }
    }
})
