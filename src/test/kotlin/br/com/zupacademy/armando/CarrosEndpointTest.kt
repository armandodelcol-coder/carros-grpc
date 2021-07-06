package br.com.zupacademy.armando

import io.grpc.ManagedChannel
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.micronaut.context.annotation.Factory
import io.micronaut.grpc.annotation.GrpcChannel
import io.micronaut.grpc.server.GrpcServerChannel
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import javax.inject.Singleton

@MicronautTest(transactional = false)
internal class CarrosEndpointTest(
    private val carroRepository: CarroRepository,
    private val grpcClient: CarrosGrpcServiceGrpc.CarrosGrpcServiceBlockingStub
) {
    @BeforeEach
    internal fun setUp() {
        carroRepository.deleteAll()
    }

    /**
     * teste de Happy path
     */
    @Test
    fun `deve cadastrar uma novo carro`() {
        // cenario
        //ação
        val response = grpcClient.adicionar(CarrosRequest.newBuilder()
            .setModelo("Gol")
            .setPlaca("AAA-1231")
            .build())
        //validação
        with(response) {
            assertNotNull(id)
            assertTrue(carroRepository.existsById(id)) // efeito colateral
        }
    }
    /**
     * teste de Quando ja existe um carro com a placa
     */
    @Test
    fun `nao deve adicionar novo carro quando placa informada ja existe`() {
        // cenario
        val placa = "ABC-3375"
        carroRepository.save(Carro(modelo = "Astra", placa = placa))
        //acao
        val error = assertThrows<StatusRuntimeException> {
            grpcClient.adicionar(CarrosRequest.newBuilder()
                .setModelo("Gol")
                .setPlaca(placa)
                .build())
        }
        //validacao
        with(error) {
            assertEquals(Status.ALREADY_EXISTS.code, status.code)
            assertEquals("Já existe um veículo com essa placa.", status.description)
        }
    }
    /**
     * teste de Quando dados de entrada sao invalidos
     */
    @Test
    fun `nao deve adicionar novo carro quando dados de entrada forem invalidos`() {
        // cenario
        //acao
        val error = assertThrows<StatusRuntimeException> {
            grpcClient.adicionar(CarrosRequest.newBuilder()
                .setModelo("")
                .setPlaca("")
                .build())
        }
        //validacao
        with(error) {
            assertEquals(Status.INVALID_ARGUMENT.code, status.code)
            assertEquals("dados de entrada inválidos.", status.description)
        }
    }

    @Factory
    class Clients {
        @Singleton
        fun blockingStub(@GrpcChannel(GrpcServerChannel.NAME) channel: ManagedChannel): CarrosGrpcServiceGrpc.CarrosGrpcServiceBlockingStub? {
            return CarrosGrpcServiceGrpc.newBlockingStub(channel)
        }
    }
}