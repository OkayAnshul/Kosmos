#!/bin/bash

echo "🚀 Running RBAC Integration Tests..."
echo "===================================="
echo ""

# Run the RBAC integration test suite
./gradlew test --tests "com.example.kosmos.RbacIntegrationTest" --info

# Check exit code
if [ $? -eq 0 ]; then
    echo ""
    echo "===================================="
    echo "✅ All RBAC tests passed!"
    echo "===================================="
    echo ""
    echo "📊 Test Summary:"
    echo "  ✅ Test 1: User creation in Supabase"
    echo "  ✅ Test 2: Project creation with auto-ADMIN"
    echo "  ✅ Test 3: Adding members with roles"
    echo "  ✅ Test 4: Role hierarchy enforcement"
    echo "  ✅ Test 5: Task creation with role tracking"
    echo "  ✅ Test 6: Cannot remove last ADMIN"
    echo ""
    echo "🔍 Next Steps:"
    echo "  1. Verify data in Supabase Dashboard:"
    echo "     https://krbfvekgqbcwjgntepip.supabase.co/project/krbfvekgqbcwjgntepip/editor"
    echo ""
    echo "  2. Check tables: users, projects, project_members, tasks"
    echo ""
    echo "  3. Review TERMINAL_TEST_RESULTS.md for verification queries"
    echo ""
else
    echo ""
    echo "===================================="
    echo "❌ Some tests failed"
    echo "===================================="
    echo ""
    echo "Check the output above for error details"
    echo ""
fi
