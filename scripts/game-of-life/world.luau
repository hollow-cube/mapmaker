local world = script.Parent

function create_bit_board(width, height)
    local bits_needed = width * height
    local bytes_needed = math.ceil(bits_needed / 8)
    return buffer.create(bytes_needed), width, height
end

function get_cell(board, width, x, y)
    local bit_index = y * width + x
    return buffer.readbits(board, bit_index, 1)
end

function set_cell(board, width, x, y, value)
    local bit_index = y * width + x
    buffer.writebits(board, bit_index, 1, value and 1 or 0)
end

function count_neighbors_bitwise(board, width, height, x, y)
    local count = 0

    for dy = -1, 1 do
        for dx = -1, 1 do
            if dx ~= 0 or dy ~= 0 then -- Skip center cell
                local nx, ny = x + dx, y + dy
                if nx >= 0 and nx < width and ny >= 0 and ny < height then
                    count = count + buffer.readbits(board, ny * width + nx, 1)
                end
            end
        end
    end
    return count
end

local worldSpace = create_bit_board(64, 64)
local copySpace = create_bit_board(64, 64)
local stepTask = nil

function init()
    for x = 0, 63 do
        for z = 0, 63 do
            local active = world:GetBlock(vec(-x, 39, -z)) == Block.Stone
            set_cell(worldSpace, 64, x, z, active)
        end
    end
end

function step()
    buffer.copy(copySpace, 0, worldSpace, 0, math.ceil(64 * 64 / 8))

    for x = 0, 63 do
        for z = 0, 63 do
            local active = get_cell(copySpace, 64, x, z) == 1
            local neighbors = count_neighbors_bitwise(copySpace, 64, 64, x, z)

            local new_state = false
            if active then
                if neighbors == 2 or neighbors == 3 then
                    new_state = true -- Cell survives
                else
                    new_state = false -- Cell dies
                end
            else
                if neighbors == 3 then
                    new_state = true -- Cell becomes alive
                end
            end

            if new_state ~= active then
                set_cell(worldSpace, 64, x, z, new_state)
                world:SetBlock(vec(-x, 39, -z), new_state and Block.Stone or Block.Air)
            end
        end
    end
end

function toggleGame()
    if stepTask then
        task.cancel(stepTask)
        stepTask = nil
    else
        stepTask = task.spawn(function()
            init()
            while true do
                task.wait(2)
                step()
            end
        end)
    end
end

toggleGame()
