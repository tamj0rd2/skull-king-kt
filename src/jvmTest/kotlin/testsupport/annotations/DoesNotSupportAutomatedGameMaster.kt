package testsupport.annotations

import org.junit.jupiter.api.extension.ExtendWith

// TODO: Do I want to enable this behaviour at the domain level? It seems reasonable, since I never really _want_ there
// to be a manual game master
@Target(AnnotationTarget.CLASS)
annotation class DoesNotSupportAutomatedGameMaster

@Target(AnnotationTarget.CLASS)
@ExtendWith(AutomatedGameMasterExtension::class)
annotation class AutomatedGameMasterTests

class AutomatedGameMasterExtension : SkipCondition<DoesNotSupportAutomatedGameMaster, AutomatedGameMasterTests>(
    DoesNotSupportAutomatedGameMaster::class,
    AutomatedGameMasterTests::class
)
