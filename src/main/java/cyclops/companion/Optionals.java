package cyclops.companion;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.concurrent.CompletableFuture;
import java.util.function.*;
import java.util.stream.Stream;

import com.aol.cyclops2.hkt.Higher;
import cyclops.typeclasses.*;

import cyclops.control.Xor;
import cyclops.typeclasses.Active;
import cyclops.typeclasses.InstanceDefinitions;
import cyclops.function.Fn3;
import cyclops.function.Fn4;
import cyclops.function.Monoid;
import cyclops.function.Reducer;
import cyclops.monads.Witness.optional;
import cyclops.monads.WitnessType;
import cyclops.monads.transformers.OptionalT;
import cyclops.typeclasses.comonad.Comonad;
import cyclops.typeclasses.foldable.Foldable;
import cyclops.typeclasses.foldable.Unfoldable;
import cyclops.typeclasses.functor.Functor;
import cyclops.typeclasses.instances.General;
import cyclops.typeclasses.monad.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.reactivestreams.Publisher;

import cyclops.monads.AnyM;
import cyclops.control.Maybe;
import cyclops.stream.ReactiveSeq;
import com.aol.cyclops2.data.collections.extensions.CollectionX;
import cyclops.collections.mutable.ListX;
import com.aol.cyclops2.types.Value;
import cyclops.monads.Witness;

import lombok.experimental.UtilityClass;


/**
 * Utility class for working with JDK Optionals
 * 
 * @author johnmcclean
 *
 */
@UtilityClass
public class Optionals {
   public static <T,W extends WitnessType<W>> OptionalT<W, T> liftM(Optional<T> opt, W witness) {
        return OptionalT.of(witness.adapter().unit(opt));
    }
    public static <W1,T> Nested<optional,W1,T> nested(Optional<Higher<W1,T>> nested, InstanceDefinitions<W1> def2){
        return Nested.of(OptionalKind.widen(nested), Instances.definitions(),def2);
    }
    public <W1,T> Product<optional,W1,T> product(Optional<T> f, Active<W1,T> active){
        return Product.of(allTypeclasses(f),active);
    }

    public static <W1,T> Coproduct<W1,optional,T> coproduct(Optional<T> f, InstanceDefinitions<W1> def2){
        return Coproduct.right(OptionalKind.widen(f),def2, Instances.definitions());
    }
    public static <T> Active<optional,T> allTypeclasses(Optional<T> f){
        return Active.of(OptionalKind.widen(f), Instances.definitions());
    }
    public <W2,T,R> Nested<optional,W2,R> mapM(Optional<T> f, Function<? super T,? extends Higher<W2,R>> fn, InstanceDefinitions<W2> defs){
        Optional<Higher<W2, R>> x = f.map(fn);
        return nested(x,defs);

    }

    public static <T,R> R visit(Optional<T> optional, Function<? super T, ? extends R> fn, Supplier<R> s){
       return optional.isPresent() ? fn.apply(optional.get()) : s.get();
    }

    /**
     * Perform a For Comprehension over a Optional, accepting 3 generating function.
     * This results in a four level nested internal iteration over the provided Optionals.
     *
     *  <pre>
     * {@code
     *
     *   import static com.aol.cyclops2.reactor.Optionals.forEach4;
     *
    forEach4(Optional.just(1),
    a-> Optional.just(a+1),
    (a,b) -> Optional.<Integer>just(a+b),
    a                  (a,b,c) -> Optional.<Integer>just(a+b+c),
    Tuple::tuple)
     *
     * }
     * </pre>
     *
     * @param value1 top level Optional
     * @param value2 Nested Optional
     * @param value3 Nested Optional
     * @param value4 Nested Optional
     * @param yieldingFunction Generates a result per combination
     * @return Optional with a combined value generated by the yielding function
     */
    public static <T1, T2, T3, R1, R2, R3, R> Optional<R> forEach4(Optional<? extends T1> value1,
                                                                   Function<? super T1, ? extends Optional<R1>> value2,
                                                                   BiFunction<? super T1, ? super R1, ? extends Optional<R2>> value3,
                                                                   Fn3<? super T1, ? super R1, ? super R2, ? extends Optional<R3>> value4,
                                                                   Fn4<? super T1, ? super R1, ? super R2, ? super R3, ? extends R> yieldingFunction) {

        return value1.flatMap(in -> {

            Optional<R1> a = value2.apply(in);
            return a.flatMap(ina -> {
                Optional<R2> b = value3.apply(in,ina);
                return b.flatMap(inb -> {
                    Optional<R3> c = value4.apply(in,ina,inb);
                    return c.map(in2 -> yieldingFunction.apply(in, ina, inb, in2));
                });

            });

        });

    }

    /**
     *
     * Perform a For Comprehension over a Optional, accepting 3 generating function.
     * This results in a four level nested internal iteration over the provided Optionals.
     *
     * <pre>
     * {@code
     *
     *  import static com.aol.cyclops2.reactor.Optionals.forEach4;
     *
     *  forEach4(Optional.just(1),
    a-> Optional.just(a+1),
    (a,b) -> Optional.<Integer>just(a+b),
    (a,b,c) -> Optional.<Integer>just(a+b+c),
    (a,b,c,d) -> a+b+c+d <100,
    Tuple::tuple);
     *
     * }
     * </pre>
     *
     * @param value1 top level Optional
     * @param value2 Nested Optional
     * @param value3 Nested Optional
     * @param value4 Nested Optional
     * @param filterFunction A filtering function, keeps values where the predicate holds
     * @param yieldingFunction Generates a result per combination
     * @return Optional with a combined value generated by the yielding function
     */
    public static <T1, T2, T3, R1, R2, R3, R> Optional<R> forEach4(Optional<? extends T1> value1,
                                                                   Function<? super T1, ? extends Optional<R1>> value2,
                                                                   BiFunction<? super T1, ? super R1, ? extends Optional<R2>> value3,
                                                                   Fn3<? super T1, ? super R1, ? super R2, ? extends Optional<R3>> value4,
                                                                   Fn4<? super T1, ? super R1, ? super R2, ? super R3, Boolean> filterFunction,
                                                                   Fn4<? super T1, ? super R1, ? super R2, ? super R3, ? extends R> yieldingFunction) {

        return value1.flatMap(in -> {

            Optional<R1> a = value2.apply(in);
            return a.flatMap(ina -> {
                Optional<R2> b = value3.apply(in,ina);
                return b.flatMap(inb -> {
                    Optional<R3> c = value4.apply(in,ina,inb);
                    return c.filter(in2->filterFunction.apply(in,ina,inb,in2))
                            .map(in2 -> yieldingFunction.apply(in, ina, inb, in2));
                });

            });

        });

    }

    /**
     * Perform a For Comprehension over a Optional, accepting 2 generating function.
     * This results in a three level nested internal iteration over the provided Optionals.
     *
     *  <pre>
     * {@code
     *
     *   import static com.aol.cyclops2.reactor.Optionals.forEach3;
     *
    forEach3(Optional.just(1),
    a-> Optional.just(a+1),
    (a,b) -> Optional.<Integer>just(a+b),
    Tuple::tuple)
     *
     * }
     * </pre>
     *
     * @param value1 top level Optional
     * @param value2 Nested Optional
     * @param value3 Nested Optional
     * @param yieldingFunction Generates a result per combination
     * @return Optional with a combined value generated by the yielding function
     */
    public static <T1, T2, R1, R2, R> Optional<R> forEach3(Optional<? extends T1> value1,
                                                           Function<? super T1, ? extends Optional<R1>> value2,
                                                           BiFunction<? super T1, ? super R1, ? extends Optional<R2>> value3,
                                                           Fn3<? super T1, ? super R1, ? super R2, ? extends R> yieldingFunction) {

        return value1.flatMap(in -> {

            Optional<R1> a = value2.apply(in);
            return a.flatMap(ina -> {
                Optional<R2> b = value3.apply(in,ina);
                return b.map(in2 -> yieldingFunction.apply(in, ina, in2));
                });


        });

    }

    /**
     *
     * Perform a For Comprehension over a Optional, accepting 2 generating function.
     * This results in a three level nested internal iteration over the provided Optionals.
     *
     * <pre>
     * {@code
     *
     *  import static com.aol.cyclops2.reactor.Optionals.forEach3;
     *
     *  forEach3(Optional.just(1),
    a-> Optional.just(a+1),
    (a,b) -> Optional.<Integer>just(a+b),
    (a,b,c) -> a+b+c <100,
    Tuple::tuple);
     *
     * }
     * </pre>
     *
     * @param value1 top level Optional
     * @param value2 Nested Optional
     * @param value3 Nested Optional
     * @param filterFunction A filtering function, keeps values where the predicate holds
     * @param yieldingFunction Generates a result per combination
     * @return Optional with a combined value generated by the yielding function
     */
    public static <T1, T2, R1, R2, R> Optional<R> forEach3(Optional<? extends T1> value1,
                                                           Function<? super T1, ? extends Optional<R1>> value2,
                                                           BiFunction<? super T1, ? super R1, ? extends Optional<R2>> value3,
                                                           Fn3<? super T1, ? super R1, ? super R2, Boolean> filterFunction,
                                                           Fn3<? super T1, ? super R1, ? super R2, ? extends R> yieldingFunction) {

        return value1.flatMap(in -> {

            Optional<R1> a = value2.apply(in);
            return a.flatMap(ina -> {
                Optional<R2> b = value3.apply(in,ina);
                return b.filter(in2->filterFunction.apply(in,ina,in2))
                            .map(in2 -> yieldingFunction.apply(in, ina, in2));
                });



        });

    }

    /**
     * Perform a For Comprehension over a Optional, accepting a generating function.
     * This results in a two level nested internal iteration over the provided Optionals.
     *
     *  <pre>
     * {@code
     *
     *   import static com.aol.cyclops2.reactor.Optionals.forEach;
     *
    forEach(Optional.just(1),
    a-> Optional.just(a+1),
    Tuple::tuple)
     *
     * }
     * </pre>
     *
     * @param value1 top level Optional
     * @param value2 Nested Optional
     * @param yieldingFunction Generates a result per combination
     * @return Optional with a combined value generated by the yielding function
     */
    public static <T, R1, R> Optional<R> forEach2(Optional<? extends T> value1, Function<? super T, Optional<R1>> value2,
                                                 BiFunction<? super T, ? super R1, ? extends R> yieldingFunction) {

        return value1.flatMap(in -> {

            Optional<R1> a = value2.apply(in);
            return a.map(in2 -> yieldingFunction.apply(in,  in2));
            });



    }

    /**
     *
     * Perform a For Comprehension over a Optional, accepting a generating function.
     * This results in a two level nested internal iteration over the provided Optionals.
     *
     * <pre>
     * {@code
     *
     *  import static com.aol.cyclops2.reactor.Optionals.forEach;
     *
     *  forEach(Optional.just(1),
    a-> Optional.just(a+1),
    (a,b) -> Optional.<Integer>just(a+b),
    (a,b,c) -> a+b+c <100,
    Tuple::tuple);
     *
     * }
     * </pre>
     *
     * @param value1 top level Optional
     * @param value2 Nested Optional
     * @param filterFunction A filtering function, keeps values where the predicate holds
     * @param yieldingFunction Generates a result per combination
     * @return Optional with a combined value generated by the yielding function
     */
    public static <T, R1, R> Optional<R> forEach2(Optional<? extends T> value1, Function<? super T, ? extends Optional<R1>> value2,
                                                 BiFunction<? super T, ? super R1, Boolean> filterFunction,
                                                 BiFunction<? super T, ? super R1, ? extends R> yieldingFunction) {

        return value1.flatMap(in -> {

            Optional<R1> a = value2.apply(in);
            return a.filter(in2->filterFunction.apply(in,in2))
                        .map(in2 -> yieldingFunction.apply(in,  in2));
            });




    }


    public static Optional<Double> optional(OptionalDouble d){
        return d.isPresent() ? Optional.of(d.getAsDouble()) : Optional.empty();
    }
    public static Optional<Long> optional(OptionalLong l){
        return l.isPresent() ? Optional.of(l.getAsLong()) : Optional.empty();
    }
    public static Optional<Integer> optional(OptionalInt l){
        return l.isPresent() ? Optional.of(l.getAsInt()) : Optional.empty();
    }
    /**
     * Sequence operation, take a Collection of Optionals and turn it into a Optional with a Collection
     * By constrast with {@link Optionals#sequencePresent(CollectionX)}, if any Optionals are zero the result
     * is an zero Optional
     * 
     * <pre>
     * {@code
     * 
     *  Optional<Integer> just = Optional.of(10);
        Optional<Integer> none = Optional.zero();
     *  
     *  Optional<ListX<Integer>> opts = Optionals.sequence(ListX.of(just, none, Optional.of(1)));
        //Optional.zero();
     * 
     * }
     * </pre>
     * 
     * 
     * @param maybes Maybes toNested Sequence
     * @return  Maybe with a List of values
     */
    public static <T> Optional<ListX<T>> sequence(final CollectionX<Optional<T>> opts) {
        return sequence(opts.stream()).map(s -> s.to().listX());

    }
    /**
     * Sequence operation, take a Collection of Optionals and turn it into a Optional with a Collection
     * Only successes are retained. By constrast with {@link Optionals#sequence(CollectionX)} Optional#zero types are
     * tolerated and ignored.
     * 
     * <pre>
     * {@code 
     *  Optional<Integer> just = Optional.of(10);
        Optional<Integer> none = Optional.zero();
     * 
     * Optional<ListX<Integer>> maybes = Optionals.sequencePresent(ListX.of(just, none, Optional.of(1)));
       //Optional.of(ListX.of(10, 1));
     * }
     * </pre>
     * 
     * @param opts Optionals toNested Sequence
     * @return Optional with a List of values
     */
    public static <T> Optional<ListX<T>> sequencePresent(final CollectionX<Optional<T>> opts) {
       return sequence(opts.stream().filter(Optional::isPresent)).map(s->s.to().listX());
    }
    /**
     * Sequence operation, take a Collection of Optionals and turn it into a Optional with a Collection
     * By constrast with {@link Optional#sequencePresent(CollectionX)} if any Optional types are zero
     * the return type will be an zero Optional
     * 
     * <pre>
     * {@code
     * 
     *  Optional<Integer> just = Optional.of(10);
        Optional<Integer> none = Optional.zero();
     *  
     *  Optional<ListX<Integer>> maybes = Optionals.sequence(ListX.of(just, none, Optional.of(1)));
        //Optional.zero();
     * 
     * }
     * </pre>
     * 
     * 
     * @param opts Maybes toNested Sequence
     * @return  Optional with a List of values
     */
    public static <T> Optional<ReactiveSeq<T>> sequence(final Stream<Optional<T>> opts) {
        return AnyM.sequence(opts.map(AnyM::fromOptional), optional.INSTANCE)
                   .map(ReactiveSeq::fromStream)
                   .to(Witness::optional);

    }
    /**
     * Accummulating operation using the supplied Reducer (@see cyclops2.Reducers). A typical use case is toNested accumulate into a Persistent Collection type.
     * Accumulates the present results, ignores zero Optionals.
     * 
     * <pre>
     * {@code 
     *  Optional<Integer> just = Optional.of(10);
        Optional<Integer> none = Optional.zero();
        
     * Optional<PersistentSetX<Integer>> opts = Optional.accumulateJust(ListX.of(just, none, Optional.of(1)), Reducers.toPersistentSetX());
       //Optional.of(PersistentSetX.of(10, 1)));
     * 
     * }
     * </pre>
     * 
     * @param optionals Optionals toNested accumulate
     * @param reducer Reducer toNested accumulate values with
     * @return Optional with reduced value
     */
    public static <T, R> Optional<R> accumulatePresent(final CollectionX<Optional<T>> optionals, final Reducer<R> reducer) {
        return sequencePresent(optionals).map(s -> s.mapReduce(reducer));
    }
    /**
     * Accumulate the results only from those Optionals which have a value present, using the supplied mapping function toNested
     * convert the data from each Optional before reducing them using the supplied Monoid (a combining BiFunction/BinaryOperator and identity element that takes two
     * input values of the same type and returns the combined result) {@see cyclops2.Monoids }.
     * 
     * <pre>
     * {@code 
     *  Optional<Integer> just = Optional.of(10);
        Optional<Integer> none = Optional.zero();
        
     *  Optional<String> opts = Optional.accumulateJust(ListX.of(just, none, Optional.of(1)), i -> "" + i,
                                                     Monoids.stringConcat);
        //Optional.of("101")
     * 
     * }
     * </pre>
     * 
     * @param optionals Optionals toNested accumulate
     * @param mapper Mapping function toNested be applied toNested the result of each Optional
     * @param reducer Monoid toNested combine values from each Optional
     * @return Optional with reduced value
     */
    public static <T, R> Optional<R> accumulatePresent(final CollectionX<Optional<T>> optionals, final Function<? super T, R> mapper,
            final Monoid<R> reducer) {
        return sequencePresent(optionals).map(s -> s.map(mapper)
                                                 .reduce(reducer));
    }
    /**
     * Accumulate the results only from those Optionals which have a value present, using the 
     * supplied Monoid (a combining BiFunction/BinaryOperator and identity element that takes two
     * input values of the same type and returns the combined result) {@see cyclops2.Monoids }.
     * 
     * <pre>
     * {@code 
     *  Optional<Integer> just = Optional.of(10);
        Optional<Integer> none = Optional.zero();
        
     *  Optional<String> opts = Optional.accumulateJust(Monoids.stringConcat,ListX.of(just, none, Optional.of(1)), 
                                                     );
        //Optional.of("101")
     * 
     * }
     * </pre>
     * 
     * @param optionals Optionals toNested accumulate
     * @param mapper Mapping function toNested be applied toNested the result of each Optional
     * @param reducer Monoid toNested combine values from each Optional
     * @return Optional with reduced value
     */
    public static <T> Optional<T> accumulatePresent(final Monoid<T> reducer, final CollectionX<Optional<T>> optionals) {
        return sequencePresent(optionals).map(s -> s
                                                 .reduce(reducer));
    }

    /**
     * Combine an Optional with the provided value using the supplied BiFunction
     * 
     * <pre>
     * {@code 
     *  Optionals.combine(Optional.of(10),Maybe.just(20), this::add)
     *  //Optional[30]
     *  
     *  private int add(int a, int b) {
            return a + b;
        }
     *  
     * }
     * </pre>
     * @param f Optional toNested combine with a value
     * @param v Value toNested combine
     * @param fn Combining function
     * @return Optional combined with supplied value
     */
    public static <T1, T2, R> Optional<R> combine(final Optional<? extends T1> f, final Value<? extends T2> v,
            final BiFunction<? super T1, ? super T2, ? extends R> fn) {
        return narrow(Maybe.fromOptional(f)
                           .combine(v, fn)
                           .toOptional());
    }
    /**
     * Combine an Optional with the provided Optional using the supplied BiFunction
     * 
     * <pre>
     * {@code 
     *  Optionals.combine(Optional.of(10),Optional.of(20), this::add)
     *  //Optional[30]
     *  
     *  private int add(int a, int b) {
            return a + b;
        }
     *  
     * }
     * </pre>
     * 
     * @param f Optional toNested combine with a value
     * @param v Optional toNested combine
     * @param fn Combining function
     * @return Optional combined with supplied value, or zero Optional if no value present
     */
    public static <T1, T2, R> Optional<R> combine(final Optional<? extends T1> f, final Optional<? extends T2> v,
            final BiFunction<? super T1, ? super T2, ? extends R> fn) {
        return combine(f,Maybe.fromOptional(v),fn);
    }

    /**
     * Combine an Optional with the provided Iterable (selecting one element if present) using the supplied BiFunction
     * <pre>
     * {@code 
     *  Optionals.zip(Optional.of(10),Arrays.asList(20), this::add)
     *  //Optional[30]
     *  
     *  private int add(int a, int b) {
            return a + b;
        }
     *  
     * }
     * </pre>
     * @param f Optional toNested combine with takeOne element in Iterable (if present)
     * @param v Iterable toNested combine
     * @param fn Combining function
     * @return Optional combined with supplied Iterable, or zero Optional if no value present
     */
    public static <T1, T2, R> Optional<R> zip(final Optional<? extends T1> f, final Iterable<? extends T2> v,
            final BiFunction<? super T1, ? super T2, ? extends R> fn) {
        return narrow(Maybe.fromOptional(f)
                           .zip(v, fn)
                           .toOptional());
    }

    /**
     * Combine an Optional with the provided Publisher (selecting one element if present) using the supplied BiFunction
     * <pre>
     * {@code 
     *  Optionals.zip(Flux.just(10),Optional.of(10), this::add)
     *  //Optional[30]
     *  
     *  private int add(int a, int b) {
            return a + b;
        }
     *  
     * }
     * </pre> 
     * 
     * @param p Publisher toNested combine
     * @param f  Optional toNested combine with
     * @param fn Combining function
     * @return Optional combined with supplied Publisher, or zero Optional if no value present
     */
    public static <T1, T2, R> Optional<R> zip(final Publisher<? extends T2> p, final Optional<? extends T1> f,
            final BiFunction<? super T1, ? super T2, ? extends R> fn) {
        return narrow(Maybe.fromOptional(f)
                           .zipP(p, fn)
                           .toOptional());
    }
    /**
     * Narrow covariant type parameter
     * 
     * @param broad Optional with covariant type parameter
     * @return Narrowed Optional
     */
    public static <T> Optional<T> narrow(final Optional<? extends T> optional) {
        return (Optional<T>) optional;
    }

    /**
     * Companion class for creating Type Class instances for working with Optionals
     * @author johnmccleanP
     *
     */
    @UtilityClass
    public static class Instances {
        public static InstanceDefinitions<optional> definitions(){
            return new InstanceDefinitions<optional>() {
                @Override
                public <T, R> Functor<optional> functor() {
                    return Instances.functor();
                }

                @Override
                public <T> Pure<optional> unit() {
                    return Instances.unit();
                }

                @Override
                public <T, R> Applicative<optional> applicative() {
                    return Instances.applicative();
                }

                @Override
                public <T, R> Monad<optional> monad() {
                    return Instances.monad();
                }

                @Override
                public <T, R> Maybe<MonadZero<optional>> monadZero() {
                    return Maybe.just(Instances.monadZero());
                }

                @Override
                public <T> Maybe<MonadPlus<optional>> monadPlus() {
                    return Maybe.just(Instances.monadPlus());
                }

                @Override
                public <T> MonadRec<optional> monadRec() {
                    return Instances.monadRec();
                }

                @Override
                public <T> Maybe<MonadPlus<optional>> monadPlus(Monoid<Higher<optional, T>> m) {
                    return Maybe.just(Instances.monadPlus((Monoid)m));
                }

                @Override
                public <C2, T> Maybe<Traverse<optional>> traverse() {
                    return Maybe.just(Instances.traverse());
                }

                @Override
                public <T> Maybe<Foldable<optional>> foldable() {
                    return Maybe.just(Instances.foldable());
                }

                @Override
                public <T> Maybe<Comonad<optional>> comonad() {
                    return Maybe.just(Instances.comonad());
                }

                @Override
                public <T> Maybe<Unfoldable<optional>> unfoldable() {
                    return Maybe.none();
                }
            };
        }

        /**
         *
         * Transform a list, mulitplying every element by 2
         *
         * <pre>
         * {@code
         *  OptionalKind<Integer> list = Optionals.functor().map(i->i*2, OptionalKind.widen(Arrays.asOptional(1,2,3));
         *
         *  //[2,4,6]
         *
         *
         * }
         * </pre>
         *
         * An example fluent api working with Optionals
         * <pre>
         * {@code
         *   OptionalKind<Integer> list = Optionals.unit()
        .unit("hello")
        .applyHKT(h->Optionals.functor().map((String v) ->v.length(), h))
        .convert(OptionalKind::narrowK3);
         *
         * }
         * </pre>
         *
         *
         * @return A functor for Optionals
         */
        public static <T,R>Functor<optional> functor(){
            BiFunction<OptionalKind<T>,Function<? super T, ? extends R>,OptionalKind<R>> map = Instances::map;
            return General.functor(map);
        }
        /**
         * <pre>
         * {@code
         * OptionalKind<String> list = Optionals.unit()
        .unit("hello")
        .convert(OptionalKind::narrowK3);

        //Arrays.asOptional("hello"))
         *
         * }
         * </pre>
         *
         *
         * @return A factory for Optionals
         */
        public static <T> Pure<optional> unit(){
            return General.<optional,T>unit(Instances::of);
        }
        /**
         *
         * <pre>
         * {@code
         * import static com.aol.cyclops.hkt.jdk.OptionalKind.widen;
         * import static com.aol.cyclops.util.function.Lambda.l1;
         * import static java.util.Arrays.asOptional;
         *
        Optionals.zippingApplicative()
        .ap(widen(asOptional(l1(this::multiplyByTwo))),widen(asOptional(1,2,3)));
         *
         * //[2,4,6]
         * }
         * </pre>
         *
         *
         * Example fluent API
         * <pre>
         * {@code
         * OptionalKind<Function<Integer,Integer>> listFn =Optionals.unit()
         *                                                  .unit(Lambda.l1((Integer i) ->i*2))
         *                                                  .convert(OptionalKind::narrowK3);

        OptionalKind<Integer> list = Optionals.unit()
        .unit("hello")
        .applyHKT(h->Optionals.functor().map((String v) ->v.length(), h))
        .applyHKT(h->Optionals.applicative().ap(listFn, h))
        .convert(OptionalKind::narrowK3);

        //Arrays.asOptional("hello".length()*2))
         *
         * }
         * </pre>
         *
         *
         * @return A zipper for Optionals
         */
        public static <T,R> Applicative<optional> applicative(){
            BiFunction<OptionalKind< Function<T, R>>,OptionalKind<T>,OptionalKind<R>> ap = Instances::ap;
            return General.applicative(functor(), unit(), ap);
        }
        /**
         *
         * <pre>
         * {@code
         * import static com.aol.cyclops.hkt.jdk.OptionalKind.widen;
         * OptionalKind<Integer> list  = Optionals.monad()
        .flatMap(i->widen(OptionalX.range(0,i)), widen(Arrays.asOptional(1,2,3)))
        .convert(OptionalKind::narrowK3);
         * }
         * </pre>
         *
         * Example fluent API
         * <pre>
         * {@code
         *    OptionalKind<Integer> list = Optionals.unit()
        .unit("hello")
        .applyHKT(h->Optionals.monad().flatMap((String v) ->Optionals.unit().unit(v.length()), h))
        .convert(OptionalKind::narrowK3);

        //Arrays.asOptional("hello".length())
         *
         * }
         * </pre>
         *
         * @return Type class with monad functions for Optionals
         */
        public static <T,R> Monad<optional> monad(){

            BiFunction<Higher<optional,T>,Function<? super T, ? extends Higher<optional,R>>,Higher<optional,R>> flatMap = Instances::flatMap;
            return General.monad(applicative(), flatMap);
        }
        /**
         *
         * <pre>
         * {@code
         *  OptionalKind<String> list = Optionals.unit()
        .unit("hello")
        .applyHKT(h->Optionals.monadZero().filter((String t)->t.startsWith("he"), h))
        .convert(OptionalKind::narrowK3);

        //Arrays.asOptional("hello"));
         *
         * }
         * </pre>
         *
         *
         * @return A filterable monad (with default value)
         */
        public static <T,R> MonadZero<optional> monadZero(){

            return General.monadZero(monad(), OptionalKind.empty());
        }
        public static  MonadRec<optional> monadRec() {

            return new MonadRec<optional>(){


                @Override
                public <T, R> Higher<optional, R> tailRec(T initial, Function<? super T, ? extends Higher<optional, ? extends Xor<T, R>>> fn) {
                    Optional<? extends Xor<T, R>> next[] = new Optional[1];
                    next[0] = Optional.of(Xor.secondary(initial));
                    boolean cont = true;
                    do {
                        cont = Optionals.visit(next[0],p -> p.visit(s -> {
                            next[0] = OptionalKind.narrowK(fn.apply(s));
                            return true;
                        }, pr -> false), () -> false);
                    } while (cont);
                    return OptionalKind.widen(next[0].map(Xor::get));

                }
            };


        }
        /**
         *
         * <pre>
         * {@code
         *  OptionalKind<Integer> list = Optionals.<Integer>monadPlus()
        .plus(OptionalKind.widen(Arrays.asOptional()), OptionalKind.widen(Arrays.asOptional(10)))
        .convert(OptionalKind::narrowK3);
        //Arrays.asOptional(10))
         *
         * }
         * </pre>
         * @return Type class for combining Optionals by concatenation
         */
        public static <T> MonadPlus<optional> monadPlus(){
            Monoid<Optional<T>> mn = Monoids.firstPresentOptional();
            Monoid<OptionalKind<T>> m = Monoid.of(OptionalKind.widen(mn.zero()), (f, g)-> OptionalKind.widen(
                    mn.apply(OptionalKind.narrowK(f), OptionalKind.narrowK(g))));

            Monoid<Higher<optional,T>> m2= (Monoid)m;
            return General.monadPlus(monadZero(),m2);
        }
        /**
         *
         * <pre>
         * {@code
         *  Monoid<OptionalKind<Integer>> m = Monoid.of(OptionalKind.widen(Arrays.asOptional()), (a,b)->a.isEmpty() ? b : a);
        OptionalKind<Integer> list = Optionals.<Integer>monadPlus(m)
        .plus(OptionalKind.widen(Arrays.asOptional(5)), OptionalKind.widen(Arrays.asOptional(10)))
        .convert(OptionalKind::narrowK3);
        //Arrays.asOptional(5))
         *
         * }
         * </pre>
         *
         * @param m Monoid toNested use for combining Optionals
         * @return Type class for combining Optionals
         */
        public static <T> MonadPlus<optional> monadPlus(Monoid<OptionalKind<T>> m){
            Monoid<Higher<optional,T>> m2= (Monoid)m;
            return General.monadPlus(monadZero(),m2);
        }

        /**
         * @return Type class for traversables with traverse / sequence operations
         */
        public static <C2,T> Traverse<optional> traverse(){

            return General.traverseByTraverse(applicative(), Instances::traverseA);
        }

        /**
         *
         * <pre>
         * {@code
         * int sum  = Optionals.foldable()
        .foldLeft(0, (a,b)->a+b, OptionalKind.widen(Arrays.asOptional(1,2,3,4)));

        //10
         *
         * }
         * </pre>
         *
         *
         * @return Type class for folding / reduction operations
         */
        public static <T> Foldable<optional> foldable(){
            BiFunction<Monoid<T>,Higher<optional,T>,T> foldRightFn =  (m, l)-> OptionalKind.narrow(l).orElse(m.zero());
            BiFunction<Monoid<T>,Higher<optional,T>,T> foldLeftFn = (m, l)-> OptionalKind.narrow(l).orElse(m.zero());
            return General.foldable(foldRightFn, foldLeftFn);
        }
        public static <T> Comonad<optional> comonad(){
            Function<? super Higher<optional, T>, ? extends T> extractFn = maybe -> maybe.convert(OptionalKind::narrow).get();
            return General.comonad(functor(), unit(), extractFn);
        }

        private <T> OptionalKind<T> of(T value){
            return OptionalKind.widen(Optional.of(value));
        }
        private static <T,R> OptionalKind<R> ap(OptionalKind<Function< T, R>> lt, OptionalKind<T> list){
            return OptionalKind.widen(Maybe.fromOptionalKind(lt).combine(Maybe.fromOptionalKind(list), (a, b)->a.apply(b)).toOptional());

        }
        private static <T,R> Higher<optional,R> flatMap(Higher<optional,T> lt, Function<? super T, ? extends  Higher<optional,R>> fn){
            return OptionalKind.widen(OptionalKind.narrow(lt).flatMap(fn.andThen(OptionalKind::narrowK)));
        }
        private static <T,R> OptionalKind<R> map(OptionalKind<T> lt, Function<? super T, ? extends R> fn){
            return OptionalKind.narrow(lt).map(fn);
        }


        private static <C2,T,R> Higher<C2, Higher<optional, R>> traverseA(Applicative<C2> applicative, Function<? super T, ? extends Higher<C2, R>> fn,
                                                                                Higher<optional, T> ds){
            Optional<T> opt = OptionalKind.narrowK(ds);
            return opt.isPresent() ?   applicative.map(OptionalKind::of, fn.apply(opt.get())) :
                    applicative.unit(OptionalKind.empty());
        }

    }

     /**
     * Simulates Higher Kinded Types for Optional's
     *
     * OptionalKind is a Optional and a Higher Kinded Type (optional,T)
     *
     * @author johnmcclean
     *
     * @param <T> Data type stored within the Optional
     */
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class OptionalKind<T> implements Higher<optional, T> {
        private final Optional<T> boxed;

       
        /**
         * @return An HKT encoded zero Optional
         */
        public static <T> OptionalKind<T> empty() {
            return widen(Optional.empty());
        }
        /**
         * @param value Value toNested embed in an Optional
         * @return An HKT encoded Optional
         */
        public static <T> OptionalKind<T> of(T value) {
            return widen(Optional.of(value));
        }
         public static <T> OptionalKind<T> ofNullable(T value) {
             return widen(Optional.ofNullable(value));
         }
        /**
         * Convert a Optional toNested a simulated HigherKindedType that captures Optional nature
         * and Optional element data type separately. Recover via @see OptionalKind#narrow
         *
         * If the supplied Optional implements OptionalKind it is returned already, otherwise it
         * is wrapped into a Optional implementation that does implement OptionalKind
         *
         * @param Optional Optional toNested widen toNested a OptionalKind
         * @return OptionalKind encoding HKT info about Optionals
         */
        public static <T> OptionalKind<T> widen(final Optional<T> Optional) {

            return new OptionalKind<T>(Optional);
        }
        /**
         * Convert the raw Higher Kinded Type for OptionalKind types into the OptionalKind type definition class
         *
         * @param future HKT encoded list into a OptionalKind
         * @return OptionalKind
         */
        public static <T> OptionalKind<T> narrow(final Higher<optional, T> future) {
            return (OptionalKind<T>)future;
        }
        /**
         * Convert the HigherKindedType definition for a Optional into
         *
         * @param Optional Type Constructor toNested convert back into narrowed type
         * @return Optional from Higher Kinded Type
         */
        public static <T> Optional<T> narrowK(final Higher<optional, T> Optional) {
            //has toNested be an OptionalKind as only OptionalKind can implement Higher<optional, T>
            return ((OptionalKind<T>)Optional).boxed;

        }
        public boolean isPresent() {
            return boxed.isPresent();
        }
        public T get() {
            return boxed.get();
        }
        public void ifPresent(Consumer<? super T> consumer) {
            boxed.ifPresent(consumer);
        }
        public OptionalKind<T> filter(Predicate<? super T> predicate) {
            return widen(boxed.filter(predicate));
        }
        public <U> OptionalKind<U> map(Function<? super T, ? extends U> mapper) {
            return widen(boxed.map(mapper));
        }
        public <U> Optional<U> flatMap(Function<? super T, Optional<U>> mapper) {
            return boxed.flatMap(mapper);
        }
        public T orElse(T other) {
            return boxed.orElse(other);
        }
        public T orElseGet(Supplier<? extends T> other) {
            return boxed.orElseGet(other);
        }
        public <X extends Throwable> T orElseThrow(Supplier<? extends X> exceptionSupplier) throws X {
            return boxed.orElseThrow(exceptionSupplier);
        }
        public boolean equals(Object obj) {
            return boxed.equals(obj);
        }
        public int hashCode() {
            return boxed.hashCode();
        }
        public String toString() {
            return boxed.toString();
        }

         public Active<optional,T> allTypeclasses(){
             return Active.of(this, Instances.definitions());
         }

         public <W2,R> Nested<optional,W2,R> mapM(Function<? super T,? extends Higher<W2,R>> fn, InstanceDefinitions<W2> defs){
             return Nested.of(map(fn), Instances.definitions(), defs);
         }
    }
}
