# Parallel Mapper

* класс ParallelMapperImpl, реализует интерфейс ParallelMapper. Метод `map` применяет переданую функция к каждому элементук в переданном `List` и возвращает ответ в виде `List`
```
public interface ParallelMapper extends AutoCloseable {
	/**
     * Maps function {@code f} over specified {@code args}.
     * Mapping for each element performs in parallel.
     *
     * @throws InterruptedException if calling thread was interrupted
     */
    <T, R> List<R> map(
        Function<? super T, ? extends R> f,
        List<? extends T> args
    ) throws InterruptedException;

	/** Stops all threads. All unfinished mappings leave in undefined state. */
    @Override
    void close();
}
```
* Метод run параллельно вычисляет функцию f на каждом из указанных аргументов (args).
* Метод close останавливать все рабочие потоки.
* Конструктор ParallelMapperImpl(int threads) создает threads рабочих потоков, которые могут быть использованы для распараллеливания.
	* К одному ParallelMapperImpl могут одновременно обращаться несколько клиентов.
* Задания на исполнение накапливаются в очереди и обрабатываются в порядке поступления.
* В реализации не присутсвуюет активных ожиданий.