package com.delique.core.shared.domain

interface UseCase<TInput, TOutput> {
    fun execute(input: TInput): TOutput
}

interface NoInputUseCase<TOutput> {
    fun execute(): TOutput
}

interface NoOutputUseCase<TInput> {
    fun execute(input: TInput)
}
