#!/usr/bin/env bash
set -euo pipefail

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Flags
DRY_RUN=false
BATCH_MODE=false
BUMP_TYPE=""

# Available modules (order matters for display)
MODULES=(
  "krat-pack-core"
  "krat-pack-cors"
  "krat-otel"
  "krat-otel-canonical-traces"
  "krat-logging"
  "krat-time"
  "krat-gag"
  "krat-jdbi"
  "krat-logging-testlib"
  "krat-time-testlib"
  "krat-otel-testlib"
  "krat-kogiven"
)

# Store batch releases: "module:version"
declare -a BATCH_RELEASES=()

usage() {
  cat << EOF
Usage: $(basename "$0") [OPTIONS]

Release script for Krat modules.

OPTIONS:
  -n, --dry-run       Show what would be done without creating/pushing tags
  -b, --batch         Batch mode: select multiple modules to release
  -a, --all <type>    Release all modules with specified bump type (patch|minor|major)
  -h, --help          Show this help message

EXAMPLES:
  $(basename "$0")                  # Interactive single module release
  $(basename "$0") --dry-run        # Preview release without executing
  $(basename "$0") --batch          # Select multiple modules to release
  $(basename "$0") --all patch      # Bump all modules with patch version
  $(basename "$0") -n --all minor   # Dry-run: preview minor bump for all modules
EOF
}

get_latest_version() {
  local module=$1
  git tag --list "${module}/v*" --sort=-v:refname | head -1 | sed "s|${module}/v||" || echo "none"
}

bump_version() {
  local version=$1
  local bump_type=$2

  if [[ "$version" == "none" || -z "$version" ]]; then
    echo "0.1.0"
    return
  fi

  IFS='.' read -r major minor patch <<< "$version"

  case $bump_type in
    major) echo "$((major + 1)).0.0" ;;
    minor) echo "${major}.$((minor + 1)).0" ;;
    patch) echo "${major}.${minor}.$((patch + 1))" ;;
    *) echo "$version" ;;
  esac
}

get_unreleased_commit_count() {
  local module=$1
  local version
  version=$(get_latest_version "$module")

  if [[ "$version" == "none" || -z "$version" ]]; then
    # Never released - count all commits touching this module
    git rev-list --count HEAD -- "${module}/" 2>/dev/null || echo "0"
  else
    # Count commits since last tag
    git rev-list --count "${module}/v${version}..HEAD" -- "${module}/" 2>/dev/null || echo "0"
  fi
}

print_header() {
  echo -e "\n${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
  if [[ "$DRY_RUN" == true ]]; then
    echo -e "${BLUE}  Krat Release Script ${YELLOW}[DRY-RUN]${NC}"
  else
    echo -e "${BLUE}  Krat Release Script${NC}"
  fi
  echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}\n"
}

print_modules() {
  echo -e "${YELLOW}Available modules:${NC}\n"
  printf "%-4s %-25s %-14s %s\n" "#" "Module" "Version" "Status"
  echo "─────────────────────────────────────────────────────────────"

  local i=1
  for module in "${MODULES[@]}"; do
    local version commit_count status
    version=$(get_latest_version "$module")
    if [[ "$version" == "" ]]; then
      version="none"
    fi
    commit_count=$(get_unreleased_commit_count "$module")
    if [[ "$commit_count" -gt 0 ]]; then
      if [[ "$commit_count" -eq 1 ]]; then
        status="${CYAN}* (1 commit)${NC}"
      else
        status="${CYAN}* (${commit_count} commits)${NC}"
      fi
    else
      status=""
    fi
    printf "%-4s %-25s %-14s %b\n" "$i)" "$module" "v$version" "$status"
    ((i++))
  done
  echo ""
}

select_module() {
  local choice
  read -rp "Select module number (1-${#MODULES[@]}): " choice

  if [[ ! "$choice" =~ ^[0-9]+$ ]] || ((choice < 1 || choice > ${#MODULES[@]})); then
    echo -e "${RED}Invalid selection${NC}" >&2
    return 1
  fi

  echo "${MODULES[$((choice - 1))]}"
}

select_modules_batch() {
  echo -e "\n${CYAN}Enter module numbers separated by spaces, or 'all':${NC}" >&2
  echo -e "${CYAN}  Example: 1 2 3${NC}\n" >&2
  local choice
  read -rp "> " choice

  if [[ "$choice" == "all" ]]; then
    echo "${MODULES[*]}"
    return
  fi

  # Normalize input: replace commas with spaces
  choice="${choice//,/ }"

  local selected=()
  for num in $choice; do
    if [[ "$num" =~ ^[0-9]+$ ]] && ((num >= 1 && num <= ${#MODULES[@]})); then
      selected+=("${MODULES[$((num - 1))]}")
    else
      echo -e "${RED}Invalid: '$num' (use numbers 1-${#MODULES[@]})${NC}" >&2
    fi
  done

  if [[ ${#selected[@]} -eq 0 ]]; then
    echo -e "${RED}No valid modules selected${NC}" >&2
    return 1
  fi

  echo "${selected[*]}"
}

select_version() {
  local module=$1
  local current_version
  current_version=$(get_latest_version "$module")

  echo -e "\n${YELLOW}Version options for ${module}:${NC}" >&2
  echo -e "  Current version: ${GREEN}v${current_version:-none}${NC}\n" >&2

  local patch minor major
  patch=$(bump_version "$current_version" "patch")
  minor=$(bump_version "$current_version" "minor")
  major=$(bump_version "$current_version" "major")

  echo "  1) Patch  → v$patch" >&2
  echo "  2) Minor  → v$minor" >&2
  echo "  3) Major  → v$major" >&2
  echo "  4) Custom version" >&2
  echo "" >&2

  local choice
  read -rp "Select (1:patch, 2:minor, 3:major, 4:custom): " choice

  case $choice in
    1) echo "$patch" ;;
    2) echo "$minor" ;;
    3) echo "$major" ;;
    4)
      read -rp "Enter custom version (without 'v' prefix): " custom
      echo "$custom"
      ;;
    *)
      echo -e "${RED}Invalid selection${NC}" >&2
      return 1
      ;;
  esac
}

select_batch_bump_type() {
  echo -e "\n${YELLOW}Select version bump type for all selected modules:${NC}\n" >&2
  echo "  1) Patch" >&2
  echo "  2) Minor" >&2
  echo "  3) Major" >&2
  echo "  4) Custom" >&2
  echo "" >&2

  local choice
  read -rp "Select (1-4): " choice

  case $choice in
    1) echo "patch" ;;
    2) echo "minor" ;;
    3) echo "major" ;;
    4)
      read -rp "Enter version (without 'v' prefix): " custom
      echo "custom:$custom"
      ;;
    *)
      echo -e "${RED}Invalid selection${NC}" >&2
      return 1
      ;;
  esac
}

print_release_summary() {
  echo -e "\n${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
  if [[ "$DRY_RUN" == true ]]; then
    echo -e "${YELLOW}  Release Summary ${RED}[DRY-RUN - No changes will be made]${NC}"
  else
    echo -e "${YELLOW}  Release Summary${NC}"
  fi
  echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}\n"

  printf "  %-25s %-12s %s\n" "Module" "Version" "Tag"
  echo "  ─────────────────────────────────────────────────────────"

  for release in "${BATCH_RELEASES[@]}"; do
    local module version tag
    module="${release%%:*}"
    version="${release##*:}"
    tag="${module}/v${version}"
    printf "  %-25s ${GREEN}%-12s${NC} %s\n" "$module" "v$version" "$tag"
  done

  echo -e "\n${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}\n"
}

confirm_release() {
  if [[ "$DRY_RUN" == true ]]; then
    return 0
  fi

  read -rp "Proceed with release? (y/N): " confirm
  [[ "$confirm" =~ ^[Yy]$ ]]
}

create_and_push_tags() {
  for release in "${BATCH_RELEASES[@]}"; do
    local module version tag
    module="${release%%:*}"
    version="${release##*:}"
    tag="${module}/v${version}"

    if [[ "$DRY_RUN" == true ]]; then
      echo -e "${CYAN}[DRY-RUN]${NC} Would create tag: ${GREEN}${tag}${NC}"
      echo -e "${CYAN}[DRY-RUN]${NC} Would push tag to origin"
    else
      echo -e "\n${BLUE}Creating tag ${tag}...${NC}"
      git tag "$tag"

      echo -e "${BLUE}Pushing tag to origin...${NC}"
      git push origin "$tag"

      echo -e "${GREEN}✓ Released ${tag}${NC}"
    fi
  done

  echo ""
  if [[ "$DRY_RUN" == true ]]; then
    echo -e "${CYAN}[DRY-RUN]${NC} ${#BATCH_RELEASES[@]} release(s) would be created."
    echo -e "${CYAN}[DRY-RUN]${NC} Run without --dry-run to execute.\n"
  else
    echo -e "${GREEN}✓ All ${#BATCH_RELEASES[@]} release(s) created and pushed successfully!${NC}"
    echo -e "${GREEN}  CI will now build and publish to Maven Central.${NC}\n"
  fi
}

check_prerequisites() {
  # Check for clean working directory
  if [[ -n $(git status --porcelain) ]]; then
    echo -e "${YELLOW}Warning: Working directory is not clean.${NC}"
    read -rp "Continue anyway? (Y/n): " confirm
    if [[ "$confirm" =~ ^[Nn]$ ]]; then
      exit 0
    fi
  fi

  # Check we're on main branch
  local current_branch
  current_branch=$(git branch --show-current)
  if [[ "$current_branch" != "main" ]]; then
    echo -e "${YELLOW}Warning: You're on branch '${current_branch}', not 'main'.${NC}"
    read -rp "Continue anyway? (y/N): " confirm
    if [[ ! "$confirm" =~ ^[Yy]$ ]]; then
      exit 0
    fi
  fi
}

# Single module release flow
single_release() {
  print_modules

  local module version
  module=$(select_module) || exit 1
  version=$(select_version "$module") || exit 1

  BATCH_RELEASES+=("${module}:${version}")

  print_release_summary

  if confirm_release; then
    create_and_push_tags
  else
    echo -e "\n${YELLOW}Release cancelled.${NC}\n"
  fi
}

# Batch release flow (interactive)
batch_release() {
  print_modules

  local modules_str bump_type
  if ! modules_str=$(select_modules_batch); then
    exit 1
  fi

  bump_type=$(select_batch_bump_type) || exit 1

  for module in $modules_str; do
    local current_version new_version
    current_version=$(get_latest_version "$module")
    if [[ "$bump_type" == custom:* ]]; then
      new_version="${bump_type#custom:}"
    else
      new_version=$(bump_version "$current_version" "$bump_type")
    fi
    BATCH_RELEASES+=("${module}:${new_version}")
  done

  print_release_summary

  if confirm_release; then
    create_and_push_tags
  else
    echo -e "\n${YELLOW}Release cancelled.${NC}\n"
  fi
}

# Release all modules with specified bump type
release_all() {
  local bump_type=$1

  for module in "${MODULES[@]}"; do
    local current_version new_version
    current_version=$(get_latest_version "$module")
    new_version=$(bump_version "$current_version" "$bump_type")
    BATCH_RELEASES+=("${module}:${new_version}")
  done

  print_release_summary

  if confirm_release; then
    create_and_push_tags
  else
    echo -e "\n${YELLOW}Release cancelled.${NC}\n"
  fi
}

# Parse arguments
parse_args() {
  while [[ $# -gt 0 ]]; do
    case $1 in
      -n|--dry-run)
        DRY_RUN=true
        shift
        ;;
      -b|--batch)
        BATCH_MODE=true
        shift
        ;;
      -a|--all)
        if [[ -z "${2:-}" ]]; then
          echo -e "${RED}Error: --all requires a bump type (patch|minor|major)${NC}"
          exit 1
        fi
        if [[ ! "$2" =~ ^(patch|minor|major)$ ]]; then
          echo -e "${RED}Error: Invalid bump type '$2'. Use patch, minor, or major.${NC}"
          exit 1
        fi
        BUMP_TYPE="$2"
        shift 2
        ;;
      -h|--help)
        usage
        exit 0
        ;;
      *)
        echo -e "${RED}Unknown option: $1${NC}"
        usage
        exit 1
        ;;
    esac
  done
}

# Main flow
main() {
  parse_args "$@"
  print_header
  check_prerequisites

  if [[ -n "$BUMP_TYPE" ]]; then
    release_all "$BUMP_TYPE"
  elif [[ "$BATCH_MODE" == true ]]; then
    batch_release
  else
    single_release
  fi
}

main "$@"
