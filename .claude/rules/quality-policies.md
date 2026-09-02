# Rule: Quality Policies (Config-Driven Enforcement)

**Category:** Optimization · **Priority:** 3

Many enforcement capabilities are **default-OFF** and activated per-stack
via the `quality_policies` block in the stack manifest. This is the
three-layer decoupling model:

```
Declaration (stack manifest)      Resolution (role-resolver)         Enforcement (skill/agent)
quality_policies:            -->  _role-context.yaml.policies:  -->  reads the active boolean,
  code_reuse:                       code_reuse:                       invokes the matching
    enabled: true                    active: true                    recipe by id
    active_postures: [mod]
```

No skill, agent, or rule file contains a stack name, file extension, or
grep pattern. All specifics come from `stacks/{stack}/stack-skills.yaml`
recipes, looked up by id. This is what lets the framework generate
either `java-spring` or `python-fastapi` code from the *same* rule and
skill files — see `rules/architecture.md` Technology Agnosticism.
